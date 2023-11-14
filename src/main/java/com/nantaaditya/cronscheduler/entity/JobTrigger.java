package com.nantaaditya.cronscheduler.entity;

import com.nantaaditya.cronscheduler.job.WebClientJob;
import com.nantaaditya.cronscheduler.model.request.CreateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateJobExecutorRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "job_trigger")
public class JobTrigger extends BaseEntity {
  private String triggerName;
  private String triggerGroup;
  private String triggerCron;

  public static JobTrigger of(CreateJobExecutorRequestDTO request) {
    return JobTrigger.builder()
        .id(BaseEntity.generateId())
        .createdBy(BaseEntity.AUDITOR)
        .modifiedBy(BaseEntity.AUDITOR)
        .triggerName(request.getTriggerName())
        .triggerGroup(WebClientJob.WEB_CLIENT_JOB_GROUP)
        .triggerCron(request.getCronTriggerExpression())
        .build();
  }

  public void update(UpdateJobExecutorRequestDTO request) {
    this.setTriggerCron(request.getCronTriggerExpression());
  }
}
