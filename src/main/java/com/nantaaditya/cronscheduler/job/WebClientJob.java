package com.nantaaditya.cronscheduler.job;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nantaaditya.cronscheduler.entity.ClientRequest;
import com.nantaaditya.cronscheduler.entity.JobHistory;
import com.nantaaditya.cronscheduler.entity.JobHistoryDetail;
import com.nantaaditya.cronscheduler.model.constant.JobDataMapKey;
import com.nantaaditya.cronscheduler.model.constant.JobStatus;
import com.nantaaditya.cronscheduler.properties.JobProperties;
import com.nantaaditya.cronscheduler.repository.JobHistoryDetailRepository;
import com.nantaaditya.cronscheduler.repository.JobHistoryRepository;
import com.nantaaditya.cronscheduler.util.JsonHelper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.r2dbc.postgresql.codec.Json;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.function.Tuples;

@Slf4j
@Component
@DisallowConcurrentExecution
public class WebClientJob implements Job {

  public static final String WEB_CLIENT_JOB_GROUP = "WebClientJobGroup";
  public static final String INSTANT_WEB_CLIENT_JOB_GROUP = "InstantWebClientJobGroup";

  private WebClient webClient;

  @Autowired
  private JobProperties jobProperties;

  @Autowired
  private JobHistoryRepository jobHistoryRepository;

  @Autowired
  private JobHistoryDetailRepository jobHistoryDetailRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    String clientRequestString = (String) context.getMergedJobDataMap().get(JobDataMapKey.CLIENT_REQUEST);
    String jobExecutorId = (String) context.getMergedJobDataMap().get(JobDataMapKey.JOB_EXECUTOR_ID);
    String cronTrigger = (String) context.getMergedJobDataMap().get(JobDataMapKey.CRON_TRIGGER);

    ClientRequest clientRequest = getClientRequest(clientRequestString);

    JobHistory jobHistory = JobHistory.create(jobExecutorId, cronTrigger);
    jobHistoryRepository.save(jobHistory)
        .subscribe(result -> log.info("#JOB - starting {}", jobExecutorId));

    this.webClient = createWebClient(clientRequest);

    jobHistory.setStatus(JobStatus.RUNNING.name());
    jobHistoryRepository.save(jobHistory)
        .subscribe(result -> log.info("#JOB - running {}", jobExecutorId));

    WebClient.RequestBodySpec requestBodySpec = webClient.method(HttpMethod.valueOf(clientRequest.getHttpMethod()))
        .uri(uriBuilder -> {
          if (StringUtils.hasLength(clientRequest.getApiPath())) {
            return uriBuilder.path(clientRequest.getFullApiPath()).build();
          }
          return uriBuilder.build();
        });

    if (StringUtils.hasLength(clientRequest.getPayloadString())) {
      requestBodySpec.bodyValue(clientRequest.getPayloadString());
    }

    Mono<String> result = requestBodySpec.exchangeToMono(response -> {
          if (response.statusCode().is2xxSuccessful()) {
            return response
                .bodyToMono(String.class)
                .doOnNext(responseBody -> log.info("#JOB - result success", responseBody));
          } else {
            return response
                .bodyToMono(String.class)
                .doOnNext(responseBody -> log.info("#JOB - result failed", responseBody));
          }
        });

    result.
        flatMap(response -> {
          jobHistory.setStatus(JobStatus.FINISH.name());
          return jobHistoryRepository.save(jobHistory)
              .map(jobHistoryResult -> Tuples.of(response, jobHistoryResult));
        })
        .flatMap(tuples -> {
          JobHistoryDetail jobHistoryDetail = JobHistoryDetail.create(jobHistory.getId(),
              clientRequest, jobExecutorId, JsonHelper.toJson(Map.of("clientResponse", tuples.getT1())));
          return jobHistoryDetailRepository.save(jobHistoryDetail);
        }).subscribe();
  }

  private WebClient createWebClient(ClientRequest clientRequest) {
    JobProperties.WebClient configuration = jobProperties.getWebClient();

    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, configuration.getConnectTimeOut() * 1000)
        .responseTimeout(Duration.ofSeconds(configuration.getResponseTimeOut()))
        .doOnConnected(conn -> conn
            .addHandlerLast(new ReadTimeoutHandler(configuration.getReadTimeOut(), TimeUnit.SECONDS))
            .addHandlerLast(new WriteTimeoutHandler(configuration.getWriteTimeOut(), TimeUnit.SECONDS)
            )
        );

    return WebClient.builder()
        .baseUrl(clientRequest.getBaseUrl())
        .defaultHeaders(headers -> headers.putAll(clientRequest.getHeaders()))
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }

  @SneakyThrows
  private ClientRequest getClientRequest(String json) {
    Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

    ClientRequest request = ClientRequest.builder()
        .clientName((String) map.get("clientName"))
        .httpMethod((String) map.get("httpMethod"))
        .baseUrl((String) map.get("baseUrl"))
        .apiPath((String) map.get("apiPath"))
        .headers(Json.of(objectMapper.writeValueAsString(map.get("headers"))))
        .build();

    if (map.get("pathParams") != null) {
      request.setPathParams(Json.of(objectMapper.writeValueAsString(map.get("pathParams"))));
    }

    if (map.get("queryParams") != null) {
      request.setQueryParams(Json.of(objectMapper.writeValueAsString(map.get("queryParams"))));
    }

    if (map.get("payload") != null) {
      request.setPayload(Json.of(objectMapper.writeValueAsString(map.get("payload"))));
    }

    return request;
  }

}
