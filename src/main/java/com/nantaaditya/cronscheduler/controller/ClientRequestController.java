package com.nantaaditya.cronscheduler.controller;

import com.nantaaditya.cronscheduler.model.request.CreateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.response.ClientResponseDTO;
import com.nantaaditya.cronscheduler.model.response.Response;
import com.nantaaditya.cronscheduler.service.ClientRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping(value = "/api/client_request")
@RequiredArgsConstructor
public class ClientRequestController {

  private final ClientRequestService clientRequestService;

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @Operation(summary = "create new web client job")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "successfully create new web client job"),
      @ApiResponse(responseCode = "400", description = "failed create new web client job")
  })
  public Mono<Response<ClientResponseDTO>> create(@RequestBody CreateClientRequestDTO request) {
    return clientRequestService.create(request)
        .map(Response::ok);
  }

  @PutMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @Operation(summary = "update web client job on quartz")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "successfully update web client job"),
      @ApiResponse(responseCode = "400", description = "failed update web client job")
  })
  public Mono<Response<ClientResponseDTO>> update(@RequestBody UpdateClientRequestDTO request) {
    return clientRequestService.update(request)
        .map(Response::ok);
  }

  @GetMapping(
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @Operation(summary = "find all client request using pagination")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "get all web client job"),
      @ApiResponse(responseCode = "400", description = "failed get all web client job")
  })
  public Mono<Response<List<ClientResponseDTO>>> findAll(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return clientRequestService.findAll(page, size)
        .map(Response::ok);
  }

  @GetMapping(
      value = "/{clientId}",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @Operation(summary = "get web client job by id")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "successfully get web client job by id"),
      @ApiResponse(responseCode = "400", description = "failed get web client job by id")
  })
  public Mono<Response<ClientResponseDTO>> find(@PathVariable String clientId) {
    return clientRequestService.find(clientId)
        .map(Response::ok);
  }

  @DeleteMapping(
      value = "/{clientId}",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @Operation(summary = "delete web client job on quartz by id")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "successfully delete web client job by id"),
      @ApiResponse(responseCode = "400", description = "failed delete web client job by id")
  })
  public Mono<Response<Boolean>> delete(@PathVariable String clientId) {
    return clientRequestService.delete(clientId)
        .map(Response::ok);
  }
}
