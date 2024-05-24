package com.nantaaditya.cronscheduler.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nantaaditya.cronscheduler.entity.ClientRequest;
import com.nantaaditya.cronscheduler.entity.JobHistory;
import com.nantaaditya.cronscheduler.entity.JobHistoryDetail;
import com.nantaaditya.cronscheduler.model.constant.JobDataMapKey;
import com.nantaaditya.cronscheduler.model.constant.JobStatus;
import com.nantaaditya.cronscheduler.model.dto.EventContext;
import com.nantaaditya.cronscheduler.model.dto.JobResponse;
import com.nantaaditya.cronscheduler.model.dto.NotificationCallbackDTO;
import com.nantaaditya.cronscheduler.properties.JobProperties;
import com.nantaaditya.cronscheduler.repository.JobHistoryDetailRepository;
import com.nantaaditya.cronscheduler.repository.JobHistoryRepository;
import com.nantaaditya.cronscheduler.service.NotificationCallback;
import com.nantaaditya.cronscheduler.util.JsonHelper;
import com.nantaaditya.cronscheduler.util.ReactorEventBus;
import com.nantaaditya.cronscheduler.util.ReactorJobExecutor;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.netty.LogbookClientHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Component
public class WebClientJobListener {

  @Autowired
  private Sinks.Many<JobExecutionContext> webClientJobSink;

  @Autowired
  private ReactorEventBus reactorEventBus;

  @Autowired
  private JobProperties jobProperties;

  @Autowired
  private JobHistoryRepository jobHistoryRepository;

  @Autowired
  private JobHistoryDetailRepository jobHistoryDetailRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private NotificationCallback notificationCallback;

  @Autowired
  private Logbook logbook;

  public static final String TRACE_ID_HEADER = "X-B3-TraceId";
  public static final String PARENT_TRACE_ID_HEADER = "X-B3-ParentSpanId";
  public static final String SPAN_ID_HEADER = "X-B3-SpanId";
  public static final String SAMPLED_HEADER = "X-B3-Sampled";
  public static final String DEFAULT_CLIENT_ERROR = "client error";
  public static final String EXECUTOR_TIMEOUT_ERROR = "executor timeout";
  public static final String NO_RESPONSE = "client no response";

  @EventListener(ApplicationReadyEvent.class)
  public Disposable webClientJob() {
    JobProperties.WebClient configuration = jobProperties.getWebClient();

    return reactorEventBus.consume(webClientJobSink, Schedulers.boundedElastic())
        .map(EventContext::from)
        .flatMap(this::createJobHistory)
        .flatMap(tuples -> ReactorJobExecutor.execute(
            this::execute,
            tuples,
            Mono.just(new JobResponse(tuples.getT1(), tuples.getT2(), getClientRequest(tuples.getT2()), EXECUTOR_TIMEOUT_ERROR)),
            Duration.ofSeconds(configuration.getResponseTimeOut())
          )
        )
        .flatMap(this::handleResponse)
        .subscribe(
            success -> log.info("#WebClientJob - success"),
            error -> log.error("#WebClientJob - error, ", error)
        );
  }

  private Mono<Tuple2<JobHistory, EventContext>> createJobHistory(EventContext eventContext) {
    return jobHistoryRepository.save(JobHistory.create(eventContext.jobExecutorId(), eventContext.cronTrigger()))
        .map(jobHistory -> Tuples.of(jobHistory, eventContext))
        .doOnNext(tuples -> {
          continueContextTrace(eventContext);
          log.info("#JOB - starting {}", eventContext.jobExecutorId());
        });
  }

  private void continueContextTrace(EventContext eventContext) {
    String traceId = eventContext.traceId();
    String spanId = eventContext.spanId();

    MDC.put(JobDataMapKey.TRACE_ID, traceId);
    MDC.put(JobDataMapKey.SPAN_ID, spanId);
  }

  private Mono<JobResponse> execute(Tuple2<JobHistory, EventContext> tuples) {
    JobHistory jobHistory = tuples.getT1();
    EventContext eventContext = tuples.getT2();
    ClientRequest clientRequest = getClientRequest(eventContext);

    return Mono.fromSupplier(() -> Tuples.of(jobHistory, createWebClient(clientRequest, eventContext)))
      .flatMap(this::updateJobHistory)
      .doOnNext(tuple -> log.info("#JOB - running {}", eventContext.jobExecutorId()))
      .map(tuple -> composeRequest(tuple, clientRequest))
      .flatMap(tuple -> call(tuple, eventContext, clientRequest))
      .map(tuple -> new JobResponse(tuple.getT1(), eventContext, clientRequest, tuple.getT2()));
  }

  private WebClient createWebClient(ClientRequest clientRequest, EventContext eventContext) {
    JobProperties.WebClient configuration = jobProperties.getWebClient();
    int jobTimeOutInMillis = clientRequest.getTimeoutInMillis();

    int connectTimeOut = jobTimeOutInMillis == 0 ?
        configuration.getConnectTimeOut() * 1000: jobTimeOutInMillis;
    Duration responseTimeOut = jobTimeOutInMillis == 0 ?
        Duration.ofSeconds(configuration.getResponseTimeOut()) : Duration.ofMillis(jobTimeOutInMillis);
    ReadTimeoutHandler readTimeoutHandler = jobTimeOutInMillis == 0 ?
        new ReadTimeoutHandler(configuration.getReadTimeOut(), TimeUnit.SECONDS) : new ReadTimeoutHandler(jobTimeOutInMillis, TimeUnit.MILLISECONDS);
    WriteTimeoutHandler writeTimeoutHandler = jobTimeOutInMillis == 0 ?
        new WriteTimeoutHandler(configuration.getWriteTimeOut(), TimeUnit.SECONDS) : new WriteTimeoutHandler(jobTimeOutInMillis, TimeUnit.MILLISECONDS);

    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeOut)
        .responseTimeout(responseTimeOut)
        .doOnConnected(conn -> conn
            .addHandlerLast(readTimeoutHandler)
            .addHandlerLast(writeTimeoutHandler)
            .addHandlerLast(new LogbookClientHandler(logbook))
        );

    return WebClient.builder()
        .baseUrl(clientRequest.getBaseUrl())
        .defaultHeaders(headers -> composeHttpHeaders(headers, clientRequest.getHeaders(), eventContext))
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }

  private Mono<Tuple2<JobHistory, WebClient>> updateJobHistory(Tuple2<JobHistory, WebClient> tuples) {
    JobHistory jobHistory = tuples.getT1();
    jobHistory.setStatus(JobStatus.RUNNING.name());

    return jobHistoryRepository.save(jobHistory)
        .map(jh -> Tuples.of(jh, tuples.getT2()));
  }

  private Tuple2<JobHistory, WebClient.RequestBodySpec> composeRequest(Tuple2<JobHistory, WebClient> tuples, ClientRequest clientRequest) {
    WebClient.RequestBodySpec requestBodySpec = tuples.getT2().method(HttpMethod.valueOf(clientRequest.getHttpMethod()))
        .uri(uriBuilder -> {
          if (StringUtils.hasLength(clientRequest.getApiPath())) {
            return uriBuilder.path(clientRequest.getFullApiPath()).build();
          }
          return uriBuilder.build();
        });

    if (StringUtils.hasLength(clientRequest.getPayloadString())) {
      requestBodySpec.bodyValue(clientRequest.getPayloadString());
    }

    return Tuples.of(tuples.getT1(), requestBodySpec);
  }

  private Mono<Tuple2<JobHistory, String>> call(Tuple2<JobHistory, WebClient.RequestBodySpec> tuples,
      EventContext eventContext, ClientRequest clientRequest) {

    String jobExecutorId = eventContext.jobExecutorId();
    String cronTrigger = eventContext.cronTrigger();

    return tuples.getT2()
        .exchangeToMono(response -> {
          if (response.statusCode().is2xxSuccessful()) {
            return response
              .bodyToMono(String.class)
              .defaultIfEmpty(NO_RESPONSE)
              .flatMap(responseBody -> notificationCallback.notifySuccess(
                  new NotificationCallbackDTO(jobExecutorId, cronTrigger, clientRequest, responseBody)
                )
                .map(notificationResponse -> Tuples.of(tuples.getT1(), responseBody))
              );
          } else {
            return response
              .bodyToMono(String.class)
              .defaultIfEmpty(NO_RESPONSE)
              .flatMap(responseBody -> notificationCallback.notifyFailed(
                  new NotificationCallbackDTO(jobExecutorId, cronTrigger, clientRequest, responseBody)
                )
                .map(notificationResponse -> Tuples.of(tuples.getT1(), responseBody))
              );
          }
        })
        .onErrorReturn(Tuples.of(tuples.getT1(), DEFAULT_CLIENT_ERROR))
        .doOnNext(tuple -> {
          continueContextTrace(eventContext);
          log.info("#JOB - result: {}", tuple.getT2());
        });
  }

  private Mono<JobHistoryDetail> handleResponse(JobResponse response) {
    EventContext eventContext = response.eventContext();
    ClientRequest clientRequest = response.clientRequest();
    String responseBody = response.responseBody();
    JobHistory jobHistory = response.jobHistory();
    String jobExecutorId = eventContext.jobExecutorId();

    return Mono.just(jobHistory)
        .doOnNext(jh -> jh.setStatus(JobStatus.FINISH.name()))
        .flatMap(jobHistoryRepository::save)
        .flatMap(jh -> {
          JobHistoryDetail jobHistoryDetail = JobHistoryDetail.create(
              jh.getId(),
              clientRequest,
              jobExecutorId,
              JsonHelper.toJson(Map.of("clientResponse", responseBody))
          );
          return jobHistoryDetailRepository.save(jobHistoryDetail);
        });
  }

  private void composeHttpHeaders(HttpHeaders httpHeaders, Map<String, List<String>> headers, EventContext eventContext) {
    httpHeaders.putAll(headers);
    httpHeaders.put(TRACE_ID_HEADER, List.of(eventContext.traceId()));
    httpHeaders.put(PARENT_TRACE_ID_HEADER, List.of(eventContext.traceId()));
    httpHeaders.put(SPAN_ID_HEADER, List.of(eventContext.spanId()));
    httpHeaders.put(SAMPLED_HEADER, List.of("1"));
  }

  @SneakyThrows
  private ClientRequest getClientRequest(EventContext eventContext) {
    Map<String, Object> map = eventContext.getClientRequest(objectMapper);

    ClientRequest request = ClientRequest.builder()
        .clientName((String) map.get("clientName"))
        .httpMethod((String) map.get("httpMethod"))
        .baseUrl((String) map.get("baseUrl"))
        .apiPath((String) map.get("apiPath"))
        .headers(JsonHelper.toJson(map.get("headers")))
        .build();

    if (map.get("pathParams") != null) {
      request.setPathParams(JsonHelper.toJson(map.get("pathParams")));
    }

    if (map.get("queryParams") != null) {
      request.setQueryParams(JsonHelper.toJson(map.get("queryParams")));
    }

    if (map.get("payload") != null) {
      request.setPayload(JsonHelper.toJson(map.get("payload")));
    }

    return request;
  }
}
