package com.nantaaditya.cronscheduler.util;

import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.internal.EncodingUtils;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TraceUtil {

  private final Tracer tracer;

  public TraceContext getTraceContext() {
    return Optional.ofNullable(tracer)
        .map(Tracer::currentTraceContext)
        .map(CurrentTraceContext::context)
        .orElseGet(this::createTraceContext);
  }

  public TraceContext createTraceContext() {
    String parentId = EncodingUtils.fromLong(IdGenerator.createLongId());
    return tracer.traceContextBuilder()
        .parentId(parentId)
        .traceId(parentId)
        .spanId(EncodingUtils.fromLong(IdGenerator.createLongId()))
        .sampled(true)
        .build();
  }
}
