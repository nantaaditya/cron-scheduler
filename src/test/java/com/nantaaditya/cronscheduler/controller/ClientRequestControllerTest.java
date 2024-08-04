package com.nantaaditya.cronscheduler.controller;

import com.nantaaditya.cronscheduler.model.request.CreateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.response.ClientResponseDTO;
import com.nantaaditya.cronscheduler.model.response.Response;
import com.nantaaditya.cronscheduler.repository.ClientRequestRepository;
import com.nantaaditya.cronscheduler.repository.JobExecutorRepository;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Slf4j
@Order(value = 1)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClientRequestControllerTest extends BaseController {

  @Autowired
  private ClientRequestRepository clientRequestRepository;

  @Autowired
  private JobExecutorRepository jobExecutorRepository;

  private CreateClientRequestDTO createRequest = CreateClientRequestDTO.builder()
      .httpMethod("GET")
      .baseUrl("https://8b3817ceae844514bd45aad137f8ee1d.api.mockbin.io")
      .apiPath("/")
      .headers(Map.of(HttpHeaders.CONTENT_TYPE, List.of(MediaType.APPLICATION_JSON_VALUE)))
      .queryParams(Map.of("size", "5"))
      .clientName("mock-bin")
      .timeoutInMillis(5000)
      .build();

  private UpdateClientRequestDTO updateRequest = UpdateClientRequestDTO.builder()
      .httpMethod("GET")
      .baseUrl("https://8b3817ceae844514bd45aad137f8ee1d.api.mockbin.io")
      .apiPath("/")
      .headers(Map.of(HttpHeaders.CONTENT_TYPE, List.of(MediaType.APPLICATION_JSON_VALUE)))
      .queryParams(Map.of("size", "5"))
      .clientName("mock-bin")
      .build();

  @Test
  @Order(1)
  void create() {
    webTestClient.post()
        .uri("/api/client_request")
        .bodyValue(createRequest)
        .exchange()
        .expectStatus().isOk()
        .returnResult(new ParameterizedTypeReference<Response<ClientResponseDTO>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(2)
  void create_clientNameNotValid() {
    webTestClient.post()
        .uri("/api/client_request")
        .bodyValue(createRequest)
        .exchange()
        .expectStatus().is4xxClientError()
        .returnResult(new ParameterizedTypeReference<Response>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(3)
  void update() {
    clientRequestRepository.findAll()
            .subscribe(result -> log.info("CR {}", result));

    jobExecutorRepository.findAll()
            .subscribe(result -> log.info("JE {}", result));

    webTestClient.put()
        .uri("/api/client_request")
        .bodyValue(updateRequest)
        .exchange()
        .expectStatus().isOk()
        .returnResult(new ParameterizedTypeReference<Response<ClientResponseDTO>>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(4)
  void update_clientNameNotExists() {
    updateRequest.setClientName("other");

    webTestClient.put()
        .uri("/api/client_request")
        .bodyValue(updateRequest)
        .exchange()
        .expectStatus().is4xxClientError()
        .returnResult(new ParameterizedTypeReference<Response>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(5)
  void findAll() {
    webTestClient.get()
        .uri("/api/client_request")
        .exchange()
        .expectStatus().isOk()
        .returnResult(new ParameterizedTypeReference<Response>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(6)
  void find() {
    webTestClient.get()
        .uri("/api/client_request/1")
        .exchange()
        .expectStatus().isOk()
        .returnResult(new ParameterizedTypeReference<Response>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(7)
  void find_failed() {
    webTestClient.get()
        .uri("/api/client_request/3")
        .exchange()
        .expectStatus().is4xxClientError()
        .returnResult(new ParameterizedTypeReference<Response>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }

  @Test
  @Order(8)
  void delete_failed() {
    webTestClient.delete()
        .uri("/api/client_request/3")
        .exchange()
        .expectStatus().is4xxClientError()
        .returnResult(new ParameterizedTypeReference<Response>() {})
        .getResponseBody()
        .doOnNext(result -> log.info("#RESPONSE - {}", result))
        .subscribe();
  }
}