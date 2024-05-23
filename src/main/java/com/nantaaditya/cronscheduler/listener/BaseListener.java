package com.nantaaditya.cronscheduler.listener;

import java.util.Map.Entry;
import java.util.Set;
import org.quartz.JobDataMap;
import org.slf4j.MDC;

public class BaseListener {

  private static final Set<String> TRACE_KEYS = Set.of("traceId", "spanId");

  protected String toString(JobDataMap jobDataMap) {
    StringBuilder sb = new StringBuilder("(");

    int i = 0;
    for (Entry<String, Object> entry : jobDataMap.entrySet()) {
      sb.append(entry.getKey()).append(":").append(entry.getValue());
      if (TRACE_KEYS.contains(entry.getKey())) {
        MDC.put(entry.getKey(), (String) entry.getValue());
      }

      if (i != jobDataMap.size() - 1) {
        sb.append(",");
      }
      i++;
    }

    sb.append(")");
    return sb.toString();
  }
}
