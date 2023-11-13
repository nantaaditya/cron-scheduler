package com.nantaaditya.cronscheduler.repository;

import com.nantaaditya.cronscheduler.entity.JobDetail;
import java.util.List;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface JobDetailRepository extends R2dbcRepository<JobDetail, String> {
  Flux<JobDetail> findByClientId(String clientId);

  Mono<Void> deleteByIdIn(List<String> jobDetailIds);
}
