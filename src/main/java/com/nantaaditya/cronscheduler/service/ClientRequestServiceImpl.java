package com.nantaaditya.cronscheduler.service;

import com.nantaaditya.cronscheduler.entity.ClientRequest;
import com.nantaaditya.cronscheduler.entity.JobExecutor;
import com.nantaaditya.cronscheduler.model.request.CreateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.request.DeleteClientRequestDTO;
import com.nantaaditya.cronscheduler.model.request.GetClientRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.response.ClientResponseDTO;
import com.nantaaditya.cronscheduler.repository.ClientRequestRepository;
import com.nantaaditya.cronscheduler.repository.CustomClientRequestRepository;
import com.nantaaditya.cronscheduler.repository.JobExecutorRepository;
import com.nantaaditya.cronscheduler.util.CopyUtil;
import com.nantaaditya.cronscheduler.util.QuartzUtil;
import jakarta.validation.Valid;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Service
@RequiredArgsConstructor
@Validated
public class ClientRequestServiceImpl implements ClientRequestService {

  private final ClientRequestRepository clientRequestRepository;

  private final JobExecutorRepository jobExecutorRepository;

  private final CustomClientRequestRepository customClientRequestRepository;

  private final QuartzUtil quartzUtil;

  private final TransactionalOperator transactionalOperator;

  @Override
  public Mono<ClientResponseDTO> create(CreateClientRequestDTO request) {
    return clientRequestRepository.save(ClientRequest.create(request))
        .map(ClientResponseDTO::of);
  }

  @Override
  public Mono<ClientResponseDTO> update(UpdateClientRequestDTO request) {
    return customClientRequestRepository.findClientRequestAndJobExecutorsByName(request.getClientName())
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
    return clientRequestRepository.findAllby(pageRequest)
        .collectList()
        .map(clientRequests -> CopyUtil.copy(clientRequests, ClientResponseDTO::new, composeResponse()));
  }

  private BiFunction<ClientRequest, ClientResponseDTO, ClientResponseDTO> composeResponse() {
    return (ClientRequest clientRequest, ClientResponseDTO response) -> {
      response.setPathParams(clientRequest.getPathParams());
      response.setQueryParams(clientRequest.getQueryParams());
      response.setHeaders(clientRequest.getHeaders());
      response.setPayload(clientRequest.getPayload());
      return response;
    };
  }

  @Override
  public Mono<ClientResponseDTO> find(@Valid GetClientRequestDTO request) {
    return clientRequestRepository.findById(request.getClientId())
        .map(ClientResponseDTO::of);
  }

  @Override
  public Mono<Boolean> delete(@Valid DeleteClientRequestDTO request) {
    return customClientRequestRepository.findClientRequestAndJobExecutorsById(request.getClientId())
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
