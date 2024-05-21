package com.nantaaditya.cronscheduler.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nantaaditya.cronscheduler.entity.ClientRequest;
import com.nantaaditya.cronscheduler.entity.JobHistory;
import com.nantaaditya.cronscheduler.entity.JobHistoryDetail;
import com.nantaaditya.cronscheduler.model.constant.JobDataMapKey;
import com.nantaaditya.cronscheduler.model.constant.JobStatus;
import com.nantaaditya.cronscheduler.model.dto.NotificationCallbackDTO;
import com.nantaaditya.cronscheduler.properties.JobProperties;
import com.nantaaditya.cronscheduler.repository.JobHistoryDetailRepository;
import com.nantaaditya.cronscheduler.repository.JobHistoryRepository;
import com.nantaaditya.cronscheduler.service.NotificationCallback;
import com.nantaaditya.cronscheduler.util.IdGenerator;
import com.nantaaditya.cronscheduler.util.JsonHelper;
import com.nantaaditya.cronscheduler.util.ReactorEventBus;
import com.nantaaditya.cronscheduler.util.ReactorJobExecutor;
import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
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
import reactor.netty.transport.logging.AdvancedByteBufFormat;
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

  @EventListener(ApplicationReadyEvent.class)
  public Disposable webClientJob() {
    JobProperties.WebClient configuration = jobProperties.getWebClient();

    return reactorEventBus.consume(webClientJobSink, Schedulers.boundedElastic())
        .flatMap(context -> ReactorJobExecutor.execute(
            this::execute,
            context,
            Duration.ofSeconds(configuration.getResponseTimeOut())
        ))
        .subscribe(
            success -> log.info("#WebClientJob - success"),
            error -> log.error("#WebClientJob - error, ", error)
        );
  }

  private Mono<JobHistoryDetail> execute(JobExecutionContext context) {
    String clientRequestString = (String) context.getMergedJobDataMap().get(JobDataMapKey.CLIENT_REQUEST);
    String jobExecutorId = (String) context.getMergedJobDataMap().get(JobDataMapKey.JOB_EXECUTOR_ID);
    String cronTrigger = (String) context.getMergedJobDataMap().get(JobDataMapKey.CRON_TRIGGER);

    ClientRequest clientRequest = getClientRequest(clientRequestString);

    return jobHistoryRepository.save(JobHistory.create(jobExecutorId, cronTrigger))
      .doOnNext(jobHistory -> log.info("#JOB - starting {}", jobExecutorId))
      .map(jobHistory -> Tuples.of(jobHistory, createWebClient(clientRequest)))
      .flatMap(this::updateJobHistory)
      .doOnNext(tuples -> log.info("#JOB - running {}", jobExecutorId))
      .map(tuples -> composeRequest(tuples, clientRequest))
      .flatMap(tuples -> call(tuples, jobExecutorId, cronTrigger, clientRequest))
      .flatMap(tuples -> handleResponse(tuples, jobExecutorId, clientRequest));
  }

  private WebClient createWebClient(ClientRequest clientRequest) {
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
        .defaultHeaders(headers -> composeHttpHeaders(headers, clientRequest.getHeaders()))
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
      String jobExecutorId, String cronTrigger, ClientRequest clientRequest) {
    return tuples.getT2().exchangeToMono(response -> {
      if (response.statusCode().is2xxSuccessful()) {
        return response
          .bodyToMono(String.class)
          .defaultIfEmpty("")
          .doOnNext(responseBody -> log.info("#JOB - result success {}", responseBody))
          .flatMap(responseBody -> notificationCallback.notifySuccess(
              new NotificationCallbackDTO(jobExecutorId, cronTrigger, clientRequest, responseBody)
            )
            .map(notificationResponse -> Tuples.of(tuples.getT1(), responseBody))
          );
      } else {
        return response
          .bodyToMono(String.class)
          .defaultIfEmpty("")
          .doOnNext(responseBody -> log.info("#JOB - result failed {}", responseBody))
          .flatMap(responseBody -> notificationCallback.notifyFailed(
              new NotificationCallbackDTO(jobExecutorId, cronTrigger, clientRequest, responseBody)
            )
            .map(notificationResponse -> Tuples.of(tuples.getT1(), responseBody))
          );
      }
    });
  }

  private Mono<JobHistoryDetail> handleResponse(Tuple2<JobHistory, String> tuples,
      String jobExecutorId, ClientRequest clientRequest) {
    return Mono.just(tuples.getT2())
        .flatMap(response -> {
          JobHistory jobHistory = tuples.getT1();
          jobHistory.setStatus(JobStatus.FINISH.name());
          return jobHistoryRepository.save(jobHistory)
              .map(jobHistoryResult -> Tuples.of(response, jobHistoryResult));
        })
        .flatMap(tuple -> {
          JobHistory jobHistory = tuple.getT2();
          JobHistoryDetail jobHistoryDetail = JobHistoryDetail.create(
              jobHistory.getId(),
              clientRequest,
              jobExecutorId,
              JsonHelper.toJson(Map.of("clientResponse", tuple.getT1()))
          );
          return jobHistoryDetailRepository.save(jobHistoryDetail);
        });
  }

  private void composeHttpHeaders(HttpHeaders httpHeaders, Map<String, List<String>> headers) {
    String traceId = IdGenerator.createId();

    httpHeaders.putAll(headers);
    httpHeaders.put(TRACE_ID_HEADER, List.of(traceId));
    httpHeaders.put(PARENT_TRACE_ID_HEADER, List.of(traceId));
    httpHeaders.put(SPAN_ID_HEADER, List.of(IdGenerator.createId()));
    httpHeaders.put(SAMPLED_HEADER, List.of("1"));
  }

  @SneakyThrows
  private ClientRequest getClientRequest(String json) {
    Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

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
