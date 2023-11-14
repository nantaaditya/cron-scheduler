package com.nantaaditya.cronscheduler.service;

import com.nantaaditya.cronscheduler.model.request.CreateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.model.response.JobExecutorResponseDTO;
import reactor.core.publisher.Mono;

public interface JobExecutorService {
  Mono<JobExecutorResponseDTO> create(CreateJobExecutorRequestDTO request);

  Mono<JobExecutorResponseDTO> update(UpdateJobExecutorRequestDTO request);
}
