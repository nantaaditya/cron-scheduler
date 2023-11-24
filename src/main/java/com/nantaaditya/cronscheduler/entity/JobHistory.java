package com.nantaaditya.cronscheduler.entity;

import com.nantaaditya.cronscheduler.model.constant.JobStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Transient;
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
  private String triggerCron;

  @Transient
  private JobHistoryDetail jobHistoryDetail;

  public static JobHistory create(String jobExecutorId, String triggerCron) {
    return JobHistory.builder()
        .id(BaseEntity.generateId())
        .createdBy(BaseEntity.AUDITOR)
        .modifiedBy(BaseEntity.AUDITOR)
        .jobExecutorId(jobExecutorId)
        .executedDate(LocalDate.now())
        .executedTime(LocalTime.now())
        .status(JobStatus.STARTING.name())
        .triggerCron(triggerCron)
        .build();
  }

  public static List<JobHistory> from(List<Map<String, Object>> rows) {
    return rows.stream()
        .map(JobHistory::from)
        .collect(Collectors.toCollection(LinkedList::new));
  }
  
  public static JobHistory from(Map<String, Object> row) {
    return JobHistory.builder()
        .id((String) row.get("h_id"))
        .createdBy((String) row.get("h_created_by"))
        .createdDate((LocalDate) row.get("h_created_date"))
        .createdTime((LocalTime) row.get("h_created_time"))
        .modifiedBy((String) row.get("h_modified_by"))
        .modifiedDate((LocalDate) row.get("h_modified_date"))
        .modifiedTime((LocalTime) row.get("h_modified_time"))
        .version((Long) row.get("h_version"))
        .jobExecutorId((String) row.get("h_job_executor_id"))
        .executedDate((LocalDate) row.get("h_executed_date"))
        .executedTime((LocalTime) row.get("h_executed_time"))
        .status((String) row.get("h_status"))
        .triggerCron((String) row.get("h_trigger_cron"))
        .jobHistoryDetail(JobHistoryDetail.from(row))
        .build();
  }
}
