package com.nantaaditya.cronscheduler.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.nantaaditya.cronscheduler.model.request.CreateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.model.response.JobExecutorResponseDTO;
import com.nantaaditya.cronscheduler.model.response.Response;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

@Slf4j
@Order(value = 2)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WireMockTest(httpPort = 8000)
class JobExecutorControllerTest extends BaseController {

  private CreateJobExecutorRequestDTO createRequest = CreateJobExecutorRequestDTO.builder()
      .clientId("1")
      .jobName("example-job")
      .cronTriggerExpression("0 0/10 0 ? * * *")
      .enable(false)
      .build();

  private UpdateJobExecutorRequestDTO updateRequest = UpdateJobExecutorRequestDTO.builder()
      .jobExecutorId("1")
      .clientId("1")
      .cronTriggerExpression("0 0/15 0 ? * * *")
      .enable(true)
      .build();

  @Test
  @Order(1)
  void create() {
    webTestClient.post()
        .uri("/api/job_executor")
        .bodyValue(createRequest)
        .exchange()
        .expectStatus().isOk()
        .returnResult(new ParameterizedTypeReference<Response<JobExecutorResponseDTO>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(2)
  void create_failed() {
    webTestClient.post()
        .uri("/api/job_executor")
        .bodyValue(createRequest)
        .exchange()
        .expectStatus().is4xxClientError()
        .returnResult(new ParameterizedTypeReference<Response<JobExecutorResponseDTO>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(2)
  void create_failed2() {
    createRequest.setClientId("2");

    webTestClient.post()
        .uri("/api/job_executor")
        .bodyValue(createRequest)
        .exchange()
        .expectStatus().is4xxClientError()
        .returnResult(new ParameterizedTypeReference<Response<JobExecutorResponseDTO>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(2)
  void create_failed3() {
    createRequest.setClientId("1");
    createRequest.setJobName("example-job");

    webTestClient.post()
        .uri("/api/job_executor")
        .bodyValue(createRequest)
        .exchange()
        .expectStatus().is4xxClientError()
        .returnResult(new ParameterizedTypeReference<Response<JobExecutorResponseDTO>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(3)
  void update() {
    webTestClient.put()
        .uri("/api/job_executor")
        .bodyValue(updateRequest)
        .exchange()
        .expectStatus().isOk()
        .returnResult(new ParameterizedTypeReference<Response<JobExecutorResponseDTO>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(4)
  void update_failed() {
    updateRequest.setJobExecutorId("3");

    webTestClient.put()
        .uri("/api/job_executor")
        .bodyValue(updateRequest)
        .exchange()
        .expectStatus().is4xxClientError()
        .returnResult(new ParameterizedTypeReference<Response<JobExecutorResponseDTO>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(4)
  void update_failed2() {
    updateRequest.setJobExecutorId("1");
    updateRequest.setClientId("3");

    webTestClient.put()
        .uri("/api/job_executor")
        .bodyValue(updateRequest)
        .exchange()
        .expectStatus().is4xxClientError()
        .returnResult(new ParameterizedTypeReference<Response<JobExecutorResponseDTO>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(5)
  void findAll() {
    webTestClient.get()
        .uri("/api/job_executor")
        .exchange()
        .expectStatus().isOk()
        .returnResult(new ParameterizedTypeReference<Response<List<JobExecutorResponseDTO>>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(6)
  void find() {
    webTestClient.get()
        .uri("/api/job_executor/1")
        .exchange()
        .expectStatus().isOk()
        .returnResult(new ParameterizedTypeReference<Response<JobExecutorResponseDTO>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(7)
  void find_empty() {
    webTestClient.get()
        .uri("/api/job_executor/3")
        .exchange()
        .expectStatus().is4xxClientError()
        .returnResult(new ParameterizedTypeReference<Response<JobExecutorResponseDTO>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(8)
  void run() {
    webTestClient.get()
        .uri("/api/job_executor/1/_run")
        .exchange()
        .expectStatus().isOk()
        .returnResult(new ParameterizedTypeReference<Response<Boolean>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();

    await("running web client job")
        .pollDelay(Duration.ofSeconds(5))
        .timeout(Duration.ofSeconds(10))
        .until(() -> Boolean.TRUE);
  }

  @Test
  @Order(8)
  void run_noResponse() {
    stubFor(
        get("/api/no-response")
            .willReturn(aResponse().withStatus(HttpStatus.OK.value()))
    );

    webTestClient.get()
        .uri("/api/job_executor/2/_run")
        .exchange()
        .expectStatus().isOk()
        .returnResult(new ParameterizedTypeReference<Response<Boolean>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();

    await("running web client job")
        .pollDelay(Duration.ofSeconds(5))
        .timeout(Duration.ofSeconds(10))
        .until(() -> Boolean.TRUE);
  }

  @Test
  @Order(9)
  void run_failed() {
    webTestClient.get()
        .uri("/api/job_executor/3/_run")
        .exchange()
        .expectStatus().is4xxClientError()
        .returnResult(new ParameterizedTypeReference<Response<Boolean>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(9)
  void toggle() {
    webTestClient.put()
        .uri("/api/job_executor/1/_toggle/true")
        .exchange()
        .expectStatus().isOk()
        .returnResult(new ParameterizedTypeReference<Response<JobExecutorResponseDTO>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(10)
  void toggle_failed() {
    webTestClient.put()
        .uri("/api/job_executor/3/_toggle/true")
        .exchange()
        .expectStatus().is4xxClientError()
        .returnResult(new ParameterizedTypeReference<Response<JobExecutorResponseDTO>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(10)
  void delete() {
    webTestClient.delete()
        .uri("/api/job_executor/1")
        .exchange()
        .expectStatus().isOk()
        .returnResult(new ParameterizedTypeReference<Response<Boolean>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(11)
  void delete_failed() {
    webTestClient.delete()
        .uri("/api/job_executor/3")
        .exchange()
        .expectStatus().is4xxClientError()
        .returnResult(new ParameterizedTypeReference<Response<Boolean>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(12)
  void delete_clientRequest() {
    webTestClient.delete()
        .uri("/api/client_request/1")
        .exchange()
        .expectStatus().isOk()
        .returnResult(new ParameterizedTypeReference<Response<Boolean>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }
}