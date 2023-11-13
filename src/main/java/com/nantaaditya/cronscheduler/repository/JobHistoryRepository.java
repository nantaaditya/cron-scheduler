package com.nantaaditya.cronscheduler.repository;

import com.nantaaditya.cronscheduler.entity.JobHistory;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobHistoryRepository extends R2dbcRepository<JobHistory, String> {

}
