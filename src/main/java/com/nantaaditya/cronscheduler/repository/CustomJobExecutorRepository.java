package com.nantaaditya.cronscheduler.repository;

import com.nantaaditya.cronscheduler.entity.JobExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomJobExecutorRepository {

  private final DatabaseClient dbClient;

  private final JobExecutorRepository jobExecutorRepository;

  private static final String UPDATE_JOB_EXECUTOR = """
      UPDATE job_executor
      SET
      client_id = :clientId,
      job_data = :jobData,
      trigger_cron = :triggerCron,
      active = :active,
      modified_date = :modifiedDate,
      modified_time = :modifiedTime,
      version = :version
      """;

  public Mono<JobExecutor> updateById(JobExecutor jobExecutor) {
    return this.dbClient.sql(String.format("%s WHERE id = :id;", UPDATE_JOB_EXECUTOR))
        .bind("clientId", jobExecutor.getId())
        .bind("jobData", jobExecutor.getJobData())
        .bind("triggerCron", jobExecutor.getTriggerCron())
        .bind("active", jobExecutor.isActive())
        .bind("modifiedDate", jobExecutor.getModifiedDate())
        .bind("modifiedTime", jobExecutor.getModifiedTime())
        .bind("version", jobExecutor.getVersion())
        .bind("id", jobExecutor.getId())
        .fetch()
        .rowsUpdated()
        .doOnSuccess(rowsUpdated -> log.info("#DB_CLIENT - update {} job executor", rowsUpdated))
        .flatMap(rowsUpdated -> jobExecutorRepository.findById(jobExecutor.getId()));
  }
}
