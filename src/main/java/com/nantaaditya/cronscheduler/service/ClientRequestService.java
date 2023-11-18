package com.nantaaditya.cronscheduler.service;

import com.nantaaditya.cronscheduler.model.request.CreateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.request.DeleteClientRequestDTO;
import com.nantaaditya.cronscheduler.model.request.GetClientRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.response.ClientResponseDTO;
import java.util.List;
import reactor.core.publisher.Mono;

public interface ClientRequestService {

  /**
   * create new client request
   * @param request
   * @return
   */
  Mono<ClientResponseDTO> create(CreateClientRequestDTO request);

  /**
   * update an existing client request, and update it into scheduler
   * @param request
   * @return
   */
  Mono<ClientResponseDTO> update(UpdateClientRequestDTO request);

  Mono<List<ClientResponseDTO>> findAll(int page, int size);

  Mono<ClientResponseDTO> find(GetClientRequestDTO clientId);

  /**
   * delete an existing client request, and remove it from job detail, job executor, and scheduler
   * @param request
   * @return
   */
  Mono<Boolean> delete(DeleteClientRequestDTO request);
}
