package com.nantaaditya.cronscheduler.repository;

import com.nantaaditya.cronscheduler.entity.JobHistory;
import java.time.LocalDate;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface JobHistoryRepository extends R2dbcRepository<JobHistory, String> {
  Mono<Long> deleteByCreatedDateBefore(LocalDate date);
}
