package com.nantaaditya.cronscheduler.repository;

import com.nantaaditya.cronscheduler.entity.JobHistoryDetail;
import java.time.LocalDate;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface JobHistoryDetailRepository extends R2dbcRepository<JobHistoryDetail, String> {
  Mono<Long> deleteByCreatedDateBefore(LocalDate date);
}
