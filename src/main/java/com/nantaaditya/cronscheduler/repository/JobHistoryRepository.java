package com.nantaaditya.cronscheduler.repository;

import com.nantaaditya.cronscheduler.entity.JobHistory;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface JobHistoryRepository extends R2dbcRepository<JobHistory, String> {
  Flux<JobHistory> findByJobExecutorIdAndStatusIn(String jobExecutorId, List<String> status, Pageable pageable);
}
