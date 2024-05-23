package com.nantaaditya.cronscheduler.model.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nantaaditya.cronscheduler.model.constant.JobDataMapKey;
import java.util.Map;
import lombok.SneakyThrows;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

public record EventContext(
    String clientRequestString,
    String jobExecutorId,
    String cronTrigger,
    String traceId,
    String spanId
) {

  @SneakyThrows
  public Map<String, Object> getClientRequest(ObjectMapper objectMapper) {
    return objectMapper.readValue(
        this.clientRequestString,
        new TypeReference<Map<String, Object>>() {}
    );
  }

  public static EventContext from(JobExecutionContext context) {
    JobDataMap jobDataMap = context.getMergedJobDataMap();
    return new EventContext(
        (String) jobDataMap.get(JobDataMapKey.CLIENT_REQUEST),
        (String) jobDataMap.get(JobDataMapKey.JOB_EXECUTOR_ID),
        (String) jobDataMap.get(JobDataMapKey.CRON_TRIGGER),
        (String) jobDataMap.get(JobDataMapKey.TRACE_ID),
        (String) jobDataMap.get(JobDataMapKey.SPAN_ID)
    );
  }
}
