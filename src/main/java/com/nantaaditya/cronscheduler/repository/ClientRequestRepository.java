package com.nantaaditya.cronscheduler.repository;

import com.nantaaditya.cronscheduler.entity.ClientRequest;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ClientRequestRepository extends R2dbcRepository<ClientRequest, String> {

  Flux<ClientRequest> findByIdIn(List<String> clientIds);

  Mono<Boolean> existsByClientName(String clientName);

  Mono<ClientRequest> findByClientName(String clientName);

  Flux<ClientRequest> findAllby(Pageable pageable);
}
