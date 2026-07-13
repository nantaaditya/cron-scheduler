package com.nantaaditya.cronscheduler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.nantaaditya.cronscheduler.model.constant.JobDataMapKey;
import com.nantaaditya.cronscheduler.model.dto.EventContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

class ReactorLogContextTest {

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void withJobContextPutsAllPresentValuesIntoContextAndMdc() {
    EventContext eventContext = new EventContext("{}", "job-1", "* * * * * ?", "trace-1", "span-1", "req-1");

    Context context = ReactorLogContext.withJobContext(Context.empty(), eventContext);

    assertEquals("req-1", context.get(JobDataMapKey.REQUEST_ID));
    assertEquals("trace-1", context.get(JobDataMapKey.TRACE_ID));
    assertEquals("span-1", context.get(JobDataMapKey.SPAN_ID));
    assertEquals("req-1", MDC.get(JobDataMapKey.REQUEST_ID));
    assertEquals("trace-1", MDC.get(JobDataMapKey.TRACE_ID));
    assertEquals("span-1", MDC.get(JobDataMapKey.SPAN_ID));
  }

  @Test
  void withJobContextSkipsNullTraceAndSpanId() {
    EventContext eventContext = new EventContext("{}", "job-1", "* * * * * ?", null, null, "req-1");

    Context context = ReactorLogContext.withJobContext(Context.empty(), eventContext);

    assertFalse(context.hasKey(JobDataMapKey.TRACE_ID));
    assertFalse(context.hasKey(JobDataMapKey.SPAN_ID));
    assertEquals("req-1", context.get(JobDataMapKey.REQUEST_ID));
  }

  @Test
  void syncMdcAppliesMdcFromContextBeforeSubscribing() {
    Mono<String> mono = ReactorLogContext.syncMdc(
        Mono.fromSupplier(() -> MDC.get(JobDataMapKey.REQUEST_ID))
    );

    StepVerifier.create(mono.contextWrite(ctx -> ctx.put(JobDataMapKey.REQUEST_ID, "req-2")))
        .expectNext("req-2")
        .verifyComplete();
  }

  @Test
  void syncMdcIsNoOpWhenContextIsEmpty() {
    MDC.put(JobDataMapKey.REQUEST_ID, "stale");

    Mono<String> mono = ReactorLogContext.syncMdc(Mono.just("value"));

    StepVerifier.create(mono)
        .expectNext("value")
        .verifyComplete();
    assertEquals("stale", MDC.get(JobDataMapKey.REQUEST_ID));
  }

  @Test
  void concurrentJobsSharingAConstrainedSchedulerDoNotLeakReqIdIntoEachOther() {
    Scheduler constrainedScheduler = Schedulers.newBoundedElastic(1, 1000, "reqid-isolation-test");
    try {
      List<EventContext> jobs = IntStream.range(0, 20)
          .mapToObj(i -> new EventContext("{}", "job-" + i, "* * * * * ?", null, null, "req-" + i))
          .toList();

      Map<String, String> observedReqIdByJob = new ConcurrentHashMap<>();

      Flux<Void> pipeline = Flux.fromIterable(jobs)
          .flatMap(eventContext -> processLikeJob(eventContext, constrainedScheduler, observedReqIdByJob));

      StepVerifier.create(pipeline)
          .verifyComplete();

      assertEquals(jobs.size(), observedReqIdByJob.size());
      for (EventContext eventContext : jobs) {
        assertEquals(eventContext.reqId(), observedReqIdByJob.get(eventContext.jobExecutorId()));
      }
    } finally {
      constrainedScheduler.dispose();
    }
  }

  private Mono<Void> processLikeJob(EventContext eventContext, Scheduler scheduler,
      Map<String, String> observedReqIdByJob) {
    return Mono.just(eventContext.jobExecutorId())
        .publishOn(scheduler)
        .flatMap(jobId -> ReactorLogContext.syncMdc(Mono.<Void>fromRunnable(() ->
            observedReqIdByJob.put(jobId, MDC.get(JobDataMapKey.REQUEST_ID)))))
        .contextWrite(ctx -> ReactorLogContext.withJobContext(ctx, eventContext));
  }
}
