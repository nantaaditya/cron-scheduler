package com.nantaaditya.cronscheduler.service;

import com.nantaaditya.cronscheduler.model.request.CreateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.model.response.JobExecutorResponseDTO;
import java.util.List;
import reactor.core.publisher.Mono;

public interface JobExecutorService {
  Mono<JobExecutorResponseDTO> create(CreateJobExecutorRequestDTO request);

  Mono<JobExecutorResponseDTO> update(UpdateJobExecutorRequestDTO request);

  Mono<List<JobExecutorResponseDTO>> findAll(int page, int size);

  Mono<JobExecutorResponseDTO> findById(String jobExecutorId);

  Mono<Boolean> deleteById(String jobExecutorId);

  Mono<JobExecutorResponseDTO> toggle(String jobExecutorId, boolean enable);
}
