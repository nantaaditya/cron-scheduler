package com.nantaaditya.cronscheduler.job;

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
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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

@Slf4j
@Component
@DisallowConcurrentExecution
public class WebClientJob implements Job {

  public static final String WEB_CLIENT_JOB_GROUP = "WebClientJobGroup";

  private WebClient webClient;

  @Autowired
  private JobProperties jobProperties;

  @Autowired
  private JobHistoryRepository jobHistoryRepository;

  @Autowired
  private JobHistoryDetailRepository jobHistoryDetailRepository;

  public WebClientJob() {}

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ClientRequest clientRequest = (ClientRequest) context.get(JobDataMapKey.CLIENT_REQUEST);
    String jobExecutorId = (String) context.get(JobDataMapKey.JOB_EXECUTOR_ID);

    JobHistory jobHistory = JobHistory.of(jobExecutorId);
    jobHistoryRepository.save(jobHistory)
        .subscribe(result -> log.info("#JOB - starting {}", jobExecutorId));

    this.webClient = createWebClient(clientRequest);

    jobHistory.setStatus(JobStatus.RUNNING.name());
    jobHistoryRepository.save(jobHistory)
        .subscribe(result -> log.info("#JOB - running {}", jobExecutorId));

    WebClient.RequestBodySpec requestBodySpec = webClient.method(HttpMethod.valueOf(clientRequest.getHttpMethod()))
        .uri(uriBuilder -> uriBuilder
            .path(clientRequest.getApiPath())
            .build()
        );

    if (StringUtils.hasLength(clientRequest.getPayload())) {
      requestBodySpec.bodyValue(clientRequest.getPayload());
    }

    Mono<Boolean> result = requestBodySpec.exchangeToMono(response -> {
          if (response.statusCode().is2xxSuccessful()) {
            log.info("#JOB - result success");
            return Mono.just(Boolean.TRUE);
          } else {
            log.error("#JOB - result failed");
            return Mono.just(Boolean.FALSE);
          }
        });

    result.subscribe(response -> {
      log.info("result {}", response);

      jobHistory.setStatus(JobStatus.FINISH.name());
      jobHistoryRepository.save(jobHistory)
          .subscribe(r -> log.info("#JOB - finish {}", jobExecutorId));

      JobHistoryDetail jobHistoryDetail = JobHistoryDetail.create(jobHistory.getId(),
          clientRequest, jobExecutorId, JsonHelper.toJson(Map.of("result", response)));
      jobHistoryDetailRepository.save(jobHistoryDetail)
          .subscribe();
    });
  }

  private WebClient createWebClient(ClientRequest clientRequest) {
    JobProperties.WebClient configuration = jobProperties.getWebClient();

    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, configuration.getConnectTimeOut())
        .responseTimeout(Duration.ofMillis(configuration.getResponseTimeOut()))
        .doOnConnected(conn -> conn
            .addHandlerLast(new ReadTimeoutHandler(configuration.getReadTimeOut(), TimeUnit.MILLISECONDS))
            .addHandlerLast(new WriteTimeoutHandler(configuration.getWriteTimeOut(), TimeUnit.MILLISECONDS)
            )
        );

    return WebClient.builder()
        .baseUrl(clientRequest.getBaseUrl())
        .defaultHeaders(headers -> headers.putAll(clientRequest.getHeaders()))
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }

}
