package com.nantaaditya.cronscheduler.util;

import com.nantaaditya.cronscheduler.model.constant.JobDataMapKey;
import com.nantaaditya.cronscheduler.model.dto.EventContext;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

public class ReactorLogContext {

  private ReactorLogContext() {}

  /**
   * Builds the per-job {@link Context} used to scope {@code reqId}/{@code traceId}/{@code spanId}
   * to a single job's execution chain via {@code .contextWrite(...)}.
   *
   * <p>As a side effect, this also synchronously primes MDC with the same values on the calling
   * thread. This is intentional, not incidental: {@code .contextWrite()} runs at subscribe time,
   * before the first operator in the chain executes, so MDC is already correct even before
   * automatic context propagation has a chance to restore it on a later scheduler hop. Callers
   * should invoke this exactly once per job subscription — if the enclosing chain ever gains a
   * {@code .retry()}/{@code .repeat()}, this will re-run and re-prime MDC on whatever thread the
   * retry executes on.
   */
  public static Context withJobContext(Context context, EventContext eventContext) {
    Context updated = putIfPresent(context, JobDataMapKey.REQUEST_ID, eventContext.reqId());
    updated = putIfPresent(updated, JobDataMapKey.TRACE_ID, eventContext.traceId());
    updated = putIfPresent(updated, JobDataMapKey.SPAN_ID, eventContext.spanId());
    return updated;
  }

  public static <T> Mono<T> syncMdc(Mono<T> mono) {
    return Mono.deferContextual(contextView -> {
      applyMdc(contextView);
      return mono;
    });
  }

  public static Mono<Void> syncMdc(Runnable action) {
    return syncMdc(Mono.fromRunnable(action));
  }

  public static void applyMdc(ContextView contextView) {
    contextView.<String>getOrEmpty(JobDataMapKey.REQUEST_ID).ifPresent(value -> MDC.put(JobDataMapKey.REQUEST_ID, value));
    contextView.<String>getOrEmpty(JobDataMapKey.TRACE_ID).ifPresent(value -> MDC.put(JobDataMapKey.TRACE_ID, value));
    contextView.<String>getOrEmpty(JobDataMapKey.SPAN_ID).ifPresent(value -> MDC.put(JobDataMapKey.SPAN_ID, value));
  }

  private static Context putIfPresent(Context context, String key, String value) {
    if (value == null) {
      return context;
    }

    MDC.put(key, value);
    return context.put(key, value);
  }
}
