package com.nantaaditya.cronscheduler.service;

import com.nantaaditya.cronscheduler.entity.ClientRequest;
import com.nantaaditya.cronscheduler.entity.JobExecutor;
import com.nantaaditya.cronscheduler.model.constant.InvalidParameterException;
import com.nantaaditya.cronscheduler.model.request.CreateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.response.ClientResponseDTO;
import com.nantaaditya.cronscheduler.repository.ClientRequestRepository;
import com.nantaaditya.cronscheduler.repository.CustomClientRequestRepository;
import com.nantaaditya.cronscheduler.repository.JobExecutorRepository;
import com.nantaaditya.cronscheduler.util.CopyUtil;
import com.nantaaditya.cronscheduler.util.QuartzUtil;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Service
@RequiredArgsConstructor
public class ClientRequestServiceImpl implements ClientRequestService {

  private final ClientRequestRepository clientRequestRepository;

  private final JobExecutorRepository jobExecutorRepository;

  private final CustomClientRequestRepository customClientRequestRepository;

  private final QuartzUtil quartzUtil;

  private final TransactionalOperator transactionalOperator;

  @Override
  public Mono<ClientResponseDTO> create(CreateClientRequestDTO request) {
    return clientRequestRepository.existsByClientName(request.getClientName())
        .handle((exists, sink) -> {
          if (exists) {
            sink.error(new InvalidParameterException(Map.of("clientName", List.of("AlreadyExists")), "invalid parameter"));
          } else {
            sink.next(request);
          }
        })
        .flatMap(r -> clientRequestRepository.save(ClientRequest.create(request)))
        .map(ClientResponseDTO::of);
  }

  @Override
  public Mono<ClientResponseDTO> update(UpdateClientRequestDTO request) {
    return clientRequestRepository.existsByClientName(request.getClientName())
        .handle((exists, sink) -> {
          if (!exists) {
            sink.error(new InvalidParameterException(Map.of("clientName", List.of("NotExists")), "invalid parameter"));
          } else {
            sink.next(request);
          }
        })
        .flatMap(r -> customClientRequestRepository.findClientRequestAndJobExecutorsByName(request.getClientName()))
        .flatMap(clientRequest -> updateClientAndJobExecutor(clientRequest, request)
            .doOnSuccess(tuple -> {
              for (JobExecutor jobExecutor : tuple.getT2()) {
                quartzUtil.updateJob(jobExecutor);
              }
            })
            .map(tuples -> tuples.getT1())
        )
        .map(ClientResponseDTO::of);
  }

  private Mono<Tuple2<ClientRequest, List<JobExecutor>>> updateClientAndJobExecutor(ClientRequest clientRequest, UpdateClientRequestDTO request) {
    return clientRequestRepository.save(clientRequest.update(request))
        .flatMap(updatedClientRequest -> updateJobDetails(clientRequest.getJobExecutors(), updatedClientRequest)
            .map(jobExecutors -> Tuples.of(updatedClientRequest, jobExecutors))
        )
        .as(transactionalOperator::transactional);
  }

  private Mono<List<JobExecutor>> updateJobDetails(List<JobExecutor> jobExecutors, ClientRequest clientRequest) {
    return Flux.fromIterable(jobExecutors)
        .doOnNext(jobExecutor -> jobExecutor.putJobDataMap(clientRequest))
        .collectList()
        .flatMap(updatedJobExecutors -> jobExecutorRepository.saveAll(updatedJobExecutors).collectList());
  }

  @Override
  public Mono<List<ClientResponseDTO>> findAll(int page, int size) {
    PageRequest pageRequest = PageRequest.of(page, size);
    pageRequest.withSort(Sort.by(Direction.ASC, "created_date", "created_time"));
    return clientRequestRepository.findBy(pageRequest)
        .collectList()
        .map(clientRequests -> CopyUtil.copy(clientRequests, ClientResponseDTO::new, composeResponse()));
  }

  private BiFunction<ClientRequest, ClientResponseDTO, ClientResponseDTO> composeResponse() {
    return (ClientRequest clientRequest, ClientResponseDTO response) -> {
      response.setPathParams(clientRequest.getPathParams());
      response.setQueryParams(clientRequest.getQueryParamMap());
      response.setHeaders(clientRequest.getHeaders());
      response.setPayload(clientRequest.getPayloadString());
      return response;
    };
  }

  @Override
  public Mono<ClientResponseDTO> find(String clientId) {
    return clientRequestRepository.existsById(clientId)
        .handle((exists, sink) -> {
          if (!exists) {
            sink.error(new InvalidParameterException(Map.of("clientId", List.of("NotExists")), "invalid parameter"));
          } else {
            sink.next(clientId);
          }
        })
        .flatMap(r -> clientRequestRepository.findById(clientId))
        .map(ClientResponseDTO::of);
  }

  @Override
  public Mono<Boolean> delete(String clientId) {
    return clientRequestRepository.existsById(clientId)
        .handle((exists, sink) -> {
          if (!exists) {
            sink.error(new InvalidParameterException(Map.of("clientId", List.of("NotExists")), "invalid parameter"));
          } else {
            sink.next(clientId);
          }
        })
        .flatMap(r -> customClientRequestRepository.findClientRequestAndJobExecutorsById(clientId))
        .doOnNext(this::deleteJobExecutorAndJobDetail)
        .map(result -> Boolean.TRUE)
        .onErrorReturn(Boolean.FALSE);
  }

  private void deleteJobExecutorAndJobDetail(ClientRequest clientRequest) {
    Mono.zip(
        clientRequestRepository.deleteById(clientRequest.getId()),
        jobExecutorRepository.deleteByIdIn(getJobExecutorIds(clientRequest.getJobExecutors()))
    )
        .as(transactionalOperator::transactional)
        .doOnSuccess(tuples -> quartzUtil.removeJobs(getJobExecutorIds(clientRequest.getJobExecutors())))
        .subscribe();
  }

  private List<String> getJobExecutorIds(List<JobExecutor> jobExecutors) {
    return jobExecutors.stream()
        .map(JobExecutor::getId)
        .collect(Collectors.toList());
  }
}
