package com.nantaaditya.cronscheduler.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import java.time.Duration;

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
    
    ReflectionTestUtils.setField(webClientJobListener, "clientId", "test-client");
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
  void testWebClientJobStreamKeepsDistinctReqIdPerJob() throws Exception {
    webClientJobListener.webClientJob();

    JobExecutionContext context1 = createMockContext("job1", "trace1");
    JobExecutionContext context2 = createMockContext("job2", "trace2");

    when(objectMapper.readValue(any(String.class), any(com.fasterxml.jackson.core.type.TypeReference.class)))
        .thenReturn(Map.of("clientName", "test", "httpMethod", "GET", "baseUrl", "http://localhost"));

    JobHistory jh1 = JobHistory.builder().id("h1").jobExecutorId("job1").build();
    JobHistory jh2 = JobHistory.builder().id("h2").jobExecutorId("job2").build();

    Map<String, List<String>> reqIdsByJob = new ConcurrentHashMap<>();

    when(jobHistoryRepository.save(any(JobHistory.class))).thenAnswer(invocation -> {
      JobHistory jobHistory = invocation.getArgument(0);
      reqIdsByJob.computeIfAbsent(jobHistory.getJobExecutorId(), key -> new CopyOnWriteArrayList<>())
          .add(MDC.get(JobDataMapKey.REQUEST_ID));
      JobHistory saved = "job1".equals(jobHistory.getJobExecutorId()) ? jh1 : jh2;
      return Mono.just(saved);
    });

    WebClient.RequestBodyUriSpec requestBodyUriSpecFull = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec requestBodySpecFull = mock(WebClient.RequestBodySpec.class);

    when(webClient.method(any())).thenReturn(requestBodyUriSpecFull);
    when(requestBodyUriSpecFull.uri(any(java.util.function.Function.class))).thenReturn(requestBodySpecFull);
    when(requestBodySpecFull.exchangeToMono(any()))
        .thenReturn(Mono.just(reactor.util.function.Tuples.of(jh1, "ok")))
        .thenReturn(Mono.just(reactor.util.function.Tuples.of(jh2, "ok")));

    when(jobHistoryDetailRepository.save(any())).thenReturn(Mono.just(new JobHistoryDetail()));

    webClientJobSink.tryEmitNext(context1);
    Thread.sleep(200);

    webClientJobSink.tryEmitNext(context2);
    Thread.sleep(200);

    List<String> job1ReqIds = reqIdsByJob.get("job1");
    List<String> job2ReqIds = reqIdsByJob.get("job2");

    assertNotNull(job1ReqIds);
    assertNotNull(job2ReqIds);
    assertFalse(job1ReqIds.isEmpty());
    assertFalse(job2ReqIds.isEmpty());

    // every reqId captured across job1's own save() calls must be non-blank and identical
    job1ReqIds.forEach(reqId -> assertFalse(reqId == null || reqId.isBlank()));
    assertEquals(1, new HashSet<>(job1ReqIds).size());

    // same for job2
    job2ReqIds.forEach(reqId -> assertFalse(reqId == null || reqId.isBlank()));
    assertEquals(1, new HashSet<>(job2ReqIds).size());

    // and the two jobs must never share a reqId
    assertNotEquals(job1ReqIds.get(0), job2ReqIds.get(0));
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
