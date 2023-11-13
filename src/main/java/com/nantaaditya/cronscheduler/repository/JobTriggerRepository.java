package com.nantaaditya.cronscheduler.repository;

import com.nantaaditya.cronscheduler.entity.JobTrigger;
import java.util.List;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface JobTriggerRepository extends R2dbcRepository<JobTrigger, String> {
  Flux<JobTrigger> findByIdIn(List<String> jobTriggerIds);
}
