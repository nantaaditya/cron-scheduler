package com.nantaaditya.cronscheduler.service;

import com.nantaaditya.cronscheduler.entity.ClientRequest;
import com.nantaaditya.cronscheduler.entity.JobDetail;
import com.nantaaditya.cronscheduler.model.request.CreateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.request.DeleteClientRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.response.ClientResponseDTO;
import com.nantaaditya.cronscheduler.repository.ClientRequestRepository;
import com.nantaaditya.cronscheduler.repository.CustomClientRequestRepository;
import com.nantaaditya.cronscheduler.repository.JobDetailRepository;
import com.nantaaditya.cronscheduler.repository.JobExecutorRepository;
import com.nantaaditya.cronscheduler.repository.JobTriggerRepository;
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

@Service
@RequiredArgsConstructor
@Validated
public class ClientRequestServiceImpl implements ClientRequestService {

  private final ClientRequestRepository clientRequestRepository;

  private final JobDetailRepository jobDetailRepository;

  private final JobExecutorRepository jobExecutorRepository;

  private final JobTriggerRepository jobTriggerRepository;

  private final CustomClientRequestRepository customClientRequestRepository;

  private final QuartzUtil quartzUtil;

  private final TransactionalOperator transactionalOperator;

  @Override
  public Mono<ClientResponseDTO> create(CreateClientRequestDTO request) {
    return clientRequestRepository.save(ClientRequest.of(request))
        .map(ClientResponseDTO::of);
  }

  @Override
  public Mono<ClientResponseDTO> update(UpdateClientRequestDTO request) {
    return customClientRequestRepository.findClientRequestAndJobDetailsByName(request.getClientName())
        .flatMap(clientRequest -> updateClientAndJobDetail(clientRequest, request)
            .doOnSuccess(tuple -> {
              for (JobDetail jobDetail : tuple.getT2()) {
                quartzUtil.updateJob(jobDetail, jobDetail.isActive());
              }
            })
            .map(tuples -> tuples.getT1())
        )
        .map(ClientResponseDTO::of);
  }

  private Mono<Tuple2<ClientRequest, List<JobDetail>>> updateClientAndJobDetail(ClientRequest clientRequest, UpdateClientRequestDTO request) {
    return Mono.zip(
            clientRequestRepository.save(clientRequest.update(request)),
            updateJobDetails(clientRequest.getJobDetails(), clientRequest)
        )
        .as(transactionalOperator::transactional);
  }

  private Mono<List<JobDetail>> updateJobDetails(List<JobDetail> jobDetails, ClientRequest clientRequest) {
    return Flux.fromIterable(jobDetails)
        .doOnNext(jobDetail -> jobDetail.updateClientRequest(clientRequest))
        .collectList()
        .flatMap(updatedJobDetails -> jobDetailRepository.saveAll(updatedJobDetails).collectList());
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
  public Mono<ClientResponseDTO> find(String clientId) {
    return clientRequestRepository.findById(clientId)
        .map(ClientResponseDTO::of);
  }

  @Override
  public Mono<Boolean> delete(@Valid DeleteClientRequestDTO request) {
    return customClientRequestRepository.findClientRequestAndJobDetailsById(request.getClientId())
        .doOnNext(this::deleteJobExecutorAndJobDetail)
        .map(result -> Boolean.TRUE)
        .onErrorReturn(Boolean.FALSE);
  }

  private void deleteJobExecutorAndJobDetail(ClientRequest clientRequest) {
    Mono.zip(
        clientRequestRepository.deleteById(clientRequest.getId()),
        jobDetailRepository.deleteByIdIn(getJobDetailIds(clientRequest.getJobDetails())),
        jobTriggerRepository.deleteByIdIn(getJobTriggerIds(clientRequest.getJobDetails())),
        jobExecutorRepository.deleteByIdIn(getJobExecutorIds(clientRequest.getJobDetails()))
    )
        .as(transactionalOperator::transactional)
        .doOnSuccess(tuples -> quartzUtil.removeJobs(getJobExecutorIds(clientRequest.getJobDetails())))
        .subscribe();
  }

  private List<String> getJobTriggerIds(List<JobDetail> jobDetails) {
    return jobDetails.stream()
        .map(JobDetail::getJobTriggerId)
        .collect(Collectors.toList());
  }

  private List<String> getJobExecutorIds(List<JobDetail> jobDetails) {
    return jobDetails.stream()
        .map(JobDetail::getJobExecutorId)
        .collect(Collectors.toList());
  }

  private List<String> getJobDetailIds(List<JobDetail> jobDetails) {
    return jobDetails.stream()
        .map(JobDetail::getId)
        .collect(Collectors.toList());
  }
}
