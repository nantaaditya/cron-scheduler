package com.nantaaditya.cronscheduler.repository;

import com.nantaaditya.cronscheduler.entity.JobExecutor;
import java.util.List;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface JobExecutorRepository extends R2dbcRepository<JobExecutor, String> {
  Flux<JobExecutor> findByJobIdIn(List<String> jobIds);

  Mono<Void> deleteByIdIn(List<String> executorIds);
}
