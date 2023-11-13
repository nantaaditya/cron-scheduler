package com.nantaaditya.cronscheduler.model.dto;

import com.nantaaditya.cronscheduler.entity.JobDetail;
import com.nantaaditya.cronscheduler.entity.JobExecutor;

public record JobExecutorDetailDTO(
    JobExecutor jobExecutor,
    JobDetail jobDetail
) {

}
