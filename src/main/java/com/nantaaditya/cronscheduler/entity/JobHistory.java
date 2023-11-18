package com.nantaaditya.cronscheduler.entity;

import com.nantaaditya.cronscheduler.model.constant.JobStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "job_history")
public class JobHistory extends BaseEntity {
  private String jobExecutorId;
  private LocalDate executedDate;
  private LocalTime executedTime;
  private String status;

  public static JobHistory create(String jobExecutorId) {
    return JobHistory.builder()
        .id(BaseEntity.generateId())
        .createdBy(BaseEntity.AUDITOR)
        .modifiedBy(BaseEntity.AUDITOR)
        .jobExecutorId(jobExecutorId)
        .executedDate(LocalDate.now())
        .executedTime(LocalTime.now())
        .status(JobStatus.STARTING.name())
        .build();
  }
}
