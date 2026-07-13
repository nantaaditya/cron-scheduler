package com.nantaaditya.cronscheduler.job;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.nantaaditya.cronscheduler.util.ReactorEventBus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import reactor.core.publisher.Sinks;

@ExtendWith(MockitoExtension.class)
class WebClientJobTest {

  @InjectMocks
  private WebClientJob webClientJob;

  @Mock
  private Sinks.Many<JobExecutionContext> webClientJobSink;

  @Mock
  private ReactorEventBus reactorEventBus;

  @Mock
  private JobExecutionContext context;

  @Test
  void executePublishesContextToSink() throws Exception {
    webClientJob.execute(context);

    verify(reactorEventBus, timeout(500)).publish(webClientJobSink, context);
  }

  @Test
  void executeDoesNotPropagateWhenPublishFails() throws Exception {
    doThrow(new RuntimeException("publish failed")).when(reactorEventBus).publish(any(), any());

    assertDoesNotThrow(() -> webClientJob.execute(context));

    verify(reactorEventBus, timeout(500)).publish(webClientJobSink, context);
  }
}
