package com.nantaaditya.cronscheduler.controller;

import com.nantaaditya.cronscheduler.model.response.JobHistoryResponseDTO;
import com.nantaaditya.cronscheduler.model.response.Response;
import com.nantaaditya.cronscheduler.service.JobHistoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping(value = "/api/job_history")
@RequiredArgsConstructor
public class JobHistoryController {

  private final JobHistoryService jobHistoryService;

  @GetMapping(
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public Mono<Response<List<JobHistoryResponseDTO>>> findAll(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return jobHistoryService.findAll(page, size)
        .map(Response::ok);
  }
}
