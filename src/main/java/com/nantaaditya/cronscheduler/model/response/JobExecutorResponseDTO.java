package com.nantaaditya.cronscheduler.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.nantaaditya.cronscheduler.entity.ClientRequest;
import com.nantaaditya.cronscheduler.entity.JobExecutor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class JobExecutorResponseDTO {

  private String jobExecutorId;
  private String jobName;
  private String jobGroup;
  private String triggerCron;
  private boolean enable;
  private ClientResponseDTO clientRequest;

  public static JobExecutorResponseDTO of(JobExecutor jobExecutor, ClientRequest clientRequest) {
    return JobExecutorResponseDTO.builder()
        .jobExecutorId(jobExecutor.getId())
        .triggerCron(jobExecutor.getTriggerCron())
        .clientRequest(ClientResponseDTO.of(clientRequest))
        .jobName(jobExecutor.getJobName())
        .jobGroup(jobExecutor.getJobGroup())
        .enable(jobExecutor.isActive())
        .build();
  }

}
