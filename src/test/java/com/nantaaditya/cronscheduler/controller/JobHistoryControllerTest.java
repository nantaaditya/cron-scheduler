package com.nantaaditya.cronscheduler.controller;

import com.nantaaditya.cronscheduler.model.response.JobHistoryResponseDTO;
import com.nantaaditya.cronscheduler.model.response.Response;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.ParameterizedTypeReference;

@Slf4j
@Order(value = 3)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JobHistoryControllerTest extends BaseController {

  @Test
  @Order(1)
  void findAll() {
    webTestClient.get()
        .uri("/api/job_history")
        .exchange()
        .expectStatus().isOk()
        .returnResult(new ParameterizedTypeReference<Response<List<JobHistoryResponseDTO>>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }
}