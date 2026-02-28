package com.nantaaditya.cronscheduler.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nantaaditya.cronscheduler.entity.JobHistory;
import com.nantaaditya.cronscheduler.entity.JobHistoryDetail;
import com.nantaaditya.cronscheduler.model.constant.JobDataMapKey;
import com.nantaaditya.cronscheduler.model.dto.NotificationCallbackDTO;
import com.nantaaditya.cronscheduler.properties.JobProperties;
import com.nantaaditya.cronscheduler.repository.JobHistoryDetailRepository;
import com.nantaaditya.cronscheduler.repository.JobHistoryRepository;
import com.nantaaditya.cronscheduler.service.NotificationCallback;
import com.nantaaditya.cronscheduler.util.ReactorEventBus;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@ExtendWith(MockitoExtension.class)
class WebClientJobListenerStreamTest {

  @InjectMocks
  private WebClientJobListener webClientJobListener;

  @Spy
  private Sinks.Many<JobExecutionContext> webClientJobSink = Sinks.many().multicast().onBackpressureBuffer();

  @Spy
  private ReactorEventBus reactorEventBus = new ReactorEventBus();

  @Mock
  private JobProperties jobProperties;

  @Mock
  private JobHistoryRepository jobHistoryRepository;

  @Mock
  private JobHistoryDetailRepository jobHistoryDetailRepository;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private NotificationCallback notificationCallback;

  @Mock
  private WebClient webClient;

  @Mock
  private WebClient.Builder webClientBuilder;

  @BeforeEach
  void setUp() {
    JobProperties.WebClient webClientConfig = mock(JobProperties.WebClient.class);
    when(jobProperties.getWebClient()).thenReturn(webClientConfig);
    when(webClientConfig.getResponseTimeOut()).thenReturn(10);
    
    ReflectionTestUtils.setField(webClientJobListener, "webClient", webClient);
    
    when(webClient.mutate()).thenReturn(webClientBuilder);
    when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
    when(webClientBuilder.defaultHeaders(any(Consumer.class))).thenReturn(webClientBuilder);
    when(webClientBuilder.build()).thenReturn(webClient);
  }

  @Test
  void testWebClientJobStreamContinuesAfterErrorInCreateJobHistory() throws Exception {
    // Start the job listener stream
    webClientJobListener.webClientJob();

    // Prepare mock data for events
    JobExecutionContext context1 = createMockContext("job1", "trace1");
    JobExecutionContext context2 = createMockContext("job2", "trace2");
    
    when(objectMapper.readValue(any(String.class), any(com.fasterxml.jackson.core.type.TypeReference.class)))
        .thenReturn(Map.of("clientName", "test", "httpMethod", "GET", "baseUrl", "http://localhost"));

    // Mock createJobHistory to fail for the first event and succeed for the second
    when(jobHistoryRepository.save(any(JobHistory.class)))
        .thenReturn(Mono.error(new RuntimeException("DB Connection Failed"))) // First event fails in createJobHistory
        .thenReturn(Mono.just(new JobHistory())); // Second event succeeds in createJobHistory

    // Mock notificationCallback for the error case
    when(notificationCallback.notifyFailed(any(NotificationCallbackDTO.class)))
        .thenReturn(Mono.just(true));

    // Emit first event
    webClientJobSink.tryEmitNext(context1);
    Thread.sleep(200);

    // Emit second event
    webClientJobSink.tryEmitNext(context2);
    Thread.sleep(200);

    // Verify that jobHistoryRepository.save was called at least twice
    verify(jobHistoryRepository, atLeastOnce()).save(any(JobHistory.class));
    verify(notificationCallback, atLeastOnce()).notifyFailed(any(NotificationCallbackDTO.class));
  }

  @Test
  void testWebClientJobStreamContinuesAfterErrorInExecution() throws Exception {
    webClientJobListener.webClientJob();

    JobExecutionContext context1 = createMockContext("job1", "trace1");
    JobExecutionContext context2 = createMockContext("job2", "trace2");

    when(objectMapper.readValue(any(String.class), any(com.fasterxml.jackson.core.type.TypeReference.class)))
        .thenReturn(Map.of("clientName", "test", "httpMethod", "GET", "baseUrl", "http://localhost"));

    JobHistory jh1 = JobHistory.builder().id("h1").build();
    JobHistory jh2 = JobHistory.builder().id("h2").build();

    // 1. createJobHistory(context1) -> jh1
    // 2. execute(context1) -> updateJobHistory(jh1) -> jh1
    // 3. call(context1) -> FAIL
    when(jobHistoryRepository.save(any(JobHistory.class)))
        .thenReturn(Mono.just(jh1)) // 1
        .thenReturn(Mono.just(jh1)) // 2
        .thenReturn(Mono.just(jh2)) // context2: createJobHistory
        .thenReturn(Mono.just(jh2)); // context2: updateJobHistory

    // Mock WebClient call for the first event to FAIL
    WebClient.RequestBodyUriSpec requestBodyUriSpec1 = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec requestBodySpec1 = mock(WebClient.RequestBodySpec.class);

    // Mock WebClient call for the second event to SUCCEED
    WebClient.RequestBodyUriSpec requestBodyUriSpec2 = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec requestBodySpec2 = mock(WebClient.RequestBodySpec.class);

    when(webClient.method(any()))
        .thenReturn(requestBodyUriSpec1)
        .thenReturn(requestBodyUriSpec2);
    
    when(requestBodyUriSpec1.uri(any(java.util.function.Function.class))).thenReturn(requestBodySpec1);
    when(requestBodySpec1.exchangeToMono(any())).thenReturn(Mono.error(new RuntimeException("WebClient Call Failure")));

    when(requestBodyUriSpec2.uri(any(java.util.function.Function.class))).thenReturn(requestBodySpec2);
    when(requestBodySpec2.exchangeToMono(any())).thenReturn(Mono.just(reactor.util.function.Tuples.of(jh2, "ok")));

    when(jobHistoryDetailRepository.save(any())).thenReturn(Mono.just(new JobHistoryDetail()));

    webClientJobSink.tryEmitNext(context1);
    Thread.sleep(200);

    webClientJobSink.tryEmitNext(context2);
    Thread.sleep(200);

    verify(jobHistoryRepository, atLeastOnce()).save(any(JobHistory.class));
    // In this case, notifyFailed is NOT called because call() uses .onErrorReturn
    // But handleResponse is called for BOTH since execute() returns JobResponse even on error return.
    verify(jobHistoryDetailRepository, atLeastOnce()).save(any(JobHistoryDetail.class));
  }

  @Test
  void testWebClientJobStreamContinuesAfterErrorInHandleResponse() throws Exception {
    webClientJobListener.webClientJob();

    JobExecutionContext context1 = createMockContext("job1", "trace1");
    JobExecutionContext context2 = createMockContext("job2", "trace2");

    when(objectMapper.readValue(any(String.class), any(com.fasterxml.jackson.core.type.TypeReference.class)))
        .thenReturn(Map.of("clientName", "test", "httpMethod", "GET", "baseUrl", "http://localhost"));

    JobHistory jh1 = JobHistory.builder().id("h1").build();
    JobHistory jh2 = JobHistory.builder().id("h2").build();

    // 1. createJobHistory(context1) -> jh1
    // 2. execute(context1) -> updateJobHistory(jh1) -> jh1
    // 3. handleResponse(context1) -> jobHistoryRepository.save(jh1) -> FAIL
    // 4. createJobHistory(context2) -> jh2
    when(jobHistoryRepository.save(any(JobHistory.class)))
        .thenReturn(Mono.just(jh1)) // 1
        .thenReturn(Mono.just(jh1)) // 2
        .thenReturn(Mono.error(new RuntimeException("HandleResponse DB Failure"))) // 3
        .thenReturn(Mono.just(jh2)); // 4

    // Mock WebClient call for execute()
    WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);

    when(webClient.method(any())).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.exchangeToMono(any())).thenReturn(Mono.just(reactor.util.function.Tuples.of(jh1, "ok")));
    
    // handleResponse saves to jobHistoryDetailRepository
    when(jobHistoryDetailRepository.save(any())).thenReturn(Mono.just(new JobHistoryDetail()));

    // Mock notificationCallback for the error case
    when(notificationCallback.notifyFailed(any(NotificationCallbackDTO.class)))
        .thenReturn(Mono.just(true));
    
    webClientJobSink.tryEmitNext(context1);
    Thread.sleep(200);

    webClientJobSink.tryEmitNext(context2);
    Thread.sleep(200);

    verify(jobHistoryRepository, atLeastOnce()).save(any(JobHistory.class));
    verify(notificationCallback, atLeastOnce()).notifyFailed(any(NotificationCallbackDTO.class));
  }

  @Test
  void testWebClientJobStreamFullSuccess() throws Exception {
    webClientJobListener.webClientJob();

    JobExecutionContext context1 = createMockContext("job1", "trace1");
    JobExecutionContext context2 = createMockContext("job2", "trace2");

    when(objectMapper.readValue(any(String.class), any(com.fasterxml.jackson.core.type.TypeReference.class)))
        .thenReturn(Map.of("clientName", "test", "httpMethod", "GET", "baseUrl", "http://localhost"));

    JobHistory jh1 = JobHistory.builder().id("h1").build();
    JobHistory jh2 = JobHistory.builder().id("h2").build();

    when(jobHistoryRepository.save(any(JobHistory.class)))
        .thenReturn(Mono.just(jh1))
        .thenReturn(Mono.just(jh1))
        .thenReturn(Mono.just(jh1))
        .thenReturn(Mono.just(jh2))
        .thenReturn(Mono.just(jh2))
        .thenReturn(Mono.just(jh2));

    // Mock WebClient call for execute()
    WebClient.RequestBodyUriSpec requestBodyUriSpecFull = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec requestBodySpecFull = mock(WebClient.RequestBodySpec.class);

    when(webClient.method(any())).thenReturn(requestBodyUriSpecFull);
    when(requestBodyUriSpecFull.uri(any(java.util.function.Function.class))).thenReturn(requestBodySpecFull);
    when(requestBodySpecFull.exchangeToMono(any())).thenReturn(Mono.just(reactor.util.function.Tuples.of(jh1, "ok")));

    when(jobHistoryDetailRepository.save(any())).thenReturn(Mono.just(new JobHistoryDetail()));

    webClientJobSink.tryEmitNext(context1);
    Thread.sleep(200);

    webClientJobSink.tryEmitNext(context2);
    Thread.sleep(200);

    verify(jobHistoryRepository, atLeastOnce()).save(any(JobHistory.class));
    verify(jobHistoryDetailRepository, atLeastOnce()).save(any(JobHistoryDetail.class));
  }

  @Test
  void testWebClientJobStreamNot200HttpStatus() throws Exception {
    webClientJobListener.webClientJob();

    JobExecutionContext context1 = createMockContext("job1", "trace1");

    when(objectMapper.readValue(any(String.class), any(com.fasterxml.jackson.core.type.TypeReference.class)))
        .thenReturn(Map.of("clientName", "test", "httpMethod", "GET", "baseUrl", "http://localhost"));

    JobHistory jh1 = JobHistory.builder().id("h1").build();

    when(jobHistoryRepository.save(any(JobHistory.class)))
        .thenReturn(Mono.just(jh1));

    WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
    org.springframework.web.reactive.function.client.ClientResponse clientResponse = mock(org.springframework.web.reactive.function.client.ClientResponse.class);

    when(webClient.method(any())).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestBodySpec);
    
    // Simulate non-2xx status
    when(requestBodySpec.exchangeToMono(any())).thenAnswer(invocation -> {
      java.util.function.Function<org.springframework.web.reactive.function.client.ClientResponse, Mono<reactor.util.function.Tuple2<JobHistory, String>>> mapper = invocation.getArgument(0);
      return mapper.apply(clientResponse);
    });
    
    when(clientResponse.statusCode()).thenReturn(org.springframework.http.HttpStatus.BAD_REQUEST);
    when(clientResponse.bodyToMono(String.class)).thenReturn(Mono.just("error body"));

    when(notificationCallback.notifyFailed(any(NotificationCallbackDTO.class))).thenReturn(Mono.just(true));
    when(jobHistoryDetailRepository.save(any())).thenReturn(Mono.just(new JobHistoryDetail()));

    webClientJobSink.tryEmitNext(context1);
    Thread.sleep(200);

    verify(notificationCallback).notifyFailed(any(NotificationCallbackDTO.class));
    verify(jobHistoryDetailRepository).save(any(JobHistoryDetail.class));
  }

  private JobExecutionContext createMockContext(String jobId, String traceId) {
    JobExecutionContext context = mock(JobExecutionContext.class);
    JobDataMap dataMap = new JobDataMap(Map.of(
        JobDataMapKey.CLIENT_REQUEST, "{}",
        JobDataMapKey.JOB_EXECUTOR_ID, jobId,
        JobDataMapKey.CRON_TRIGGER, "0/5 * * * * ?",
        JobDataMapKey.TRACE_ID, traceId,
        JobDataMapKey.SPAN_ID, "span-" + jobId
    ));
    when(context.getMergedJobDataMap()).thenReturn(dataMap);
    return context;
  }
}
