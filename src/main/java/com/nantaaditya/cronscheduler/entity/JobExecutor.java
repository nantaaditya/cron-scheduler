package com.nantaaditya.cronscheduler.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "job_executor")
public class JobExecutor extends BaseEntity {
  private String jobId;
  private String triggerId;
  private boolean active;

  public static JobExecutor of(String jobId, String triggerId, boolean active) {
    return JobExecutor.builder()
        .id(BaseEntity.generateId())
        .createdBy(BaseEntity.AUDITOR)
        .modifiedBy(BaseEntity.AUDITOR)
        .jobId(jobId)
        .triggerId(triggerId)
        .active(active)
        .build();
  }
}
