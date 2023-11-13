package com.nantaaditya.cronscheduler.repository;

import com.nantaaditya.cronscheduler.entity.JobHistoryDetail;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobHistoryDetailRepository extends R2dbcRepository<JobHistoryDetail, String> {

}
