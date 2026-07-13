package com.nantaaditya.cronscheduler.repository;

import com.nantaaditya.cronscheduler.entity.JobHistoryDetail;
import java.time.LocalDate;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface JobHistoryDetailRepository extends R2dbcRepository<JobHistoryDetail, String> {
  @Modifying
  @Query("DELETE FROM job_history_detail WHERE job_history_id IN "
      + "(SELECT id FROM job_history WHERE created_date < :date)")
  Mono<Long> deleteByJobHistoryCreatedDateBefore(LocalDate date);
}
