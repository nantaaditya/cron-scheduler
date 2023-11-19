package com.nantaaditya.cronscheduler.service;

import com.nantaaditya.cronscheduler.model.request.CreateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.model.response.JobExecutorResponseDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

@Validated
public interface JobExecutorService {
  Mono<JobExecutorResponseDTO> create(@Valid CreateJobExecutorRequestDTO request);

  Mono<JobExecutorResponseDTO> update(@Valid UpdateJobExecutorRequestDTO request);

  Mono<List<JobExecutorResponseDTO>> findAll(
      @Valid @Min(value = 0, message = "NotValid") int page,
      @Valid @Min(value = 1, message = "NotValid") int size);

  Mono<JobExecutorResponseDTO> findById(@Valid @NotNull(message = "NotNull") String id);

  Mono<Boolean> deleteById(@Valid @NotNull(message = "NotNull") String id);

  Mono<JobExecutorResponseDTO> toggle(@Valid @NotNull(message = "NotNull") String id, boolean enable);

  Mono<Boolean> run(@Valid @NotNull(message = "NotNull") String id);
}
