package com.nantaaditya.cronscheduler.model.response;

import com.nantaaditya.cronscheduler.entity.ClientRequest;
import com.nantaaditya.cronscheduler.entity.JobDetail;
import com.nantaaditya.cronscheduler.entity.JobTrigger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutorResponseDTO {

  private String jobExecutorId;
  private ClientResponseDTO clientRequest;
  private JobDetailDTO jobDetail;
  private JobTriggerDTO jobTrigger;
  private boolean enable;

  public static JobExecutorResponseDTO of(String jobExecutorId, ClientRequest clientRequest,
      JobDetail jobDetail, JobTrigger jobTrigger, boolean enable) {
    return JobExecutorResponseDTO.builder()
        .jobExecutorId(jobExecutorId)
        .clientRequest(ClientResponseDTO.of(clientRequest))
        .jobDetail(new JobDetailDTO(jobDetail))
        .jobTrigger(new JobTriggerDTO(jobTrigger))
        .enable(enable)
        .build();
  }

  @Data
  @NoArgsConstructor
  public static class JobDetailDTO {
    private String jobName;
    private String jobGroup;

    public JobDetailDTO(JobDetail jobDetail) {
      this.jobName = jobDetail.getJobName();
      this.jobGroup = jobDetail.getJobGroup();
    }
  }

  @Data
  @NoArgsConstructor
  public static class JobTriggerDTO {
    private String triggerName;
    private String triggerGroup;
    private String triggerCron;

    public JobTriggerDTO(JobTrigger jobTrigger) {
      this.triggerName = jobTrigger.getTriggerName();
      this.triggerGroup = jobTrigger.getTriggerGroup();
      this.triggerCron = jobTrigger.getTriggerCron();
    }
  }
}
