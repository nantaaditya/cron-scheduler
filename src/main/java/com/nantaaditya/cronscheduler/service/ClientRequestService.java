package com.nantaaditya.cronscheduler.service;

import com.nantaaditya.cronscheduler.model.request.CreateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.response.ClientResponseDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

@Validated
public interface ClientRequestService {

  /**
   * create new client request
   * @param request
   * @return
   */
  Mono<ClientResponseDTO> create(@Valid CreateClientRequestDTO request);

  /**
   * update an existing client request, and update it into scheduler
   * @param request
   * @return
   */
  Mono<ClientResponseDTO> update(@Valid UpdateClientRequestDTO request);

  Mono<List<ClientResponseDTO>> findAll(
      @Valid @Min(value = 0, message = "NotValid") int page,
      @Valid @Min(value = 1, message = "NotValid") int size);

  Mono<ClientResponseDTO> find(@Valid @NotNull(message = "NotExists") String clientId);

  /**
   * delete an existing client request, and remove it from job detail, job executor, and scheduler
   * @param clientId
   * @return
   */
  Mono<Boolean> delete(@Valid @NotNull(message = "NotExists") String clientId);
}
