package com.nantaaditya.cronscheduler.repository;

import com.nantaaditya.cronscheduler.entity.JobHistory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomJobHistoryRepository {

  private final DatabaseClient dbClient;
  
  private static final String FIND_JOB_HISTORY_AND_DETAIL_SQL = """
      SELECT
      h.id h_id, h.created_by h_created_by, h.created_date h_created_date, h.created_time h_created_time, h.modified_by h_modified_by, h.modified_date h_modified_date, h.modified_time h_modified_time, h.version h_version,
      h.job_executor_id h_job_executor_id, h.executed_date h_executed_date, h.executed_time h_executed_time, h.status h_status, h.trigger_cron h_trigger_cron,
      d.client_request d_client_request, d.result_detail d_result_detail
      FROM job_history h
      JOIN job_history_detail d
      ON h.id=d.job_history_id
      """;

  public Mono<List<JobHistory>> findAll(int page, int size) {
    String sql = String.format("%s ORDER BY h.created_date desc, h.created_time desc LIMIT %s OFFSET %s", FIND_JOB_HISTORY_AND_DETAIL_SQL, size, (page*size));
    return dbClient.sql(sql)
        .fetch()
        .all()
        .collectList()
        .map(JobHistory::from);
  }
}
