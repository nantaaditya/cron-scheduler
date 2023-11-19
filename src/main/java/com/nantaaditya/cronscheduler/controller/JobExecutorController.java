package com.nantaaditya.cronscheduler.controller;

import com.nantaaditya.cronscheduler.model.request.CreateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.model.response.JobExecutorResponseDTO;
import com.nantaaditya.cronscheduler.model.response.Response;
import com.nantaaditya.cronscheduler.service.JobExecutorService;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/api/job_executor")
@RequiredArgsConstructor
public class JobExecutorController {

  private final JobExecutorService jobExecutorService;

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Mono<Response<JobExecutorResponseDTO>> create(@RequestBody CreateJobExecutorRequestDTO request) {
    return jobExecutorService.create(request)
        .map(Response::ok);
  }

  @PutMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Mono<Response<JobExecutorResponseDTO>> update(@RequestBody UpdateJobExecutorRequestDTO request) {
    return jobExecutorService.update(request)
        .map(Response::ok);
  }

  @GetMapping(
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Mono<Response<List<JobExecutorResponseDTO>>> findAll(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "1") int size) {
    return jobExecutorService.findAll(page, size)
        .map(Response::ok);
  }

  @GetMapping(
      value = "/{id}",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Mono<Response<JobExecutorResponseDTO>> find(@NotBlank(message = "NotBlank") String id) {
    return jobExecutorService.findById(id)
        .map(Response::ok);
  }

  @DeleteMapping(
      value = "/{id}",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Mono<Response<Boolean>> delete(@NotBlank(message = "NotBlank") @PathVariable String id) {
    return jobExecutorService.deleteById(id)
        .map(Response::ok);
  }

  @PutMapping(
      value = "/{id}/_toggle/{enable}",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Mono<Response<JobExecutorResponseDTO>> toggle(@PathVariable String id, @PathVariable boolean enable) {
    return jobExecutorService.toggle(id, enable)
        .map(Response::ok);
  }

  @GetMapping(
      value = "/{id}/_run",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Mono<Response<Boolean>> run(@PathVariable String id) {
    return jobExecutorService.run(id)
        .map(Response::ok);
  }
}
