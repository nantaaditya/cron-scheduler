package com.nantaaditya.cronscheduler.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class JobHistoryResponseDTO {
  private String id;
  private String jobExecutorId;
  private LocalDate executedDate;
  private LocalTime executedTime;
  private String status;
  private String triggerCron;
  private Map<String, Object> clientRequest;
  private Object result;
}
