package com.nantaaditya.cronscheduler.service;

import com.nantaaditya.cronscheduler.model.response.JobHistoryResponseDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

@Validated
public interface JobHistoryService {

  Mono<List<JobHistoryResponseDTO>> findAll(
      @Valid @Min(value = 0, message = "NotValid") int page,
      @Valid @Min(value = 1, message = "NotValid") int size);
}
