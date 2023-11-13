package com.nantaaditya.cronscheduler.service;

import com.nantaaditya.cronscheduler.entity.ClientRequest;
import com.nantaaditya.cronscheduler.entity.JobDetail;
import com.nantaaditya.cronscheduler.entity.JobExecutor;
import com.nantaaditya.cronscheduler.model.request.CreateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.response.ClientResponseDTO;
import com.nantaaditya.cronscheduler.repository.ClientRequestRepository;
import com.nantaaditya.cronscheduler.repository.JobDetailRepository;
import com.nantaaditya.cronscheduler.repository.JobExecutorRepository;
import com.nantaaditya.cronscheduler.util.CopyUtil;
import com.nantaaditya.cronscheduler.util.QuartzUtil;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
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

  private final JobDetailRepository jobDetailRepository;

  private final JobExecutorRepository jobExecutorRepository;

  private final QuartzUtil quartzUtil;

  private final TransactionalOperator transactionalOperator;

  @Override
  public Mono<ClientResponseDTO> create(CreateClientRequestDTO request) {
    return clientRequestRepository.save(ClientRequest.of(request))
        .map(ClientResponseDTO::of);
  }

  @Override
  public Mono<ClientResponseDTO> update(UpdateClientRequestDTO request) {
    return clientRequestRepository.findByClientName(request.getClientName())
        .flatMap(this::findClientRequestAndJobDetail)
        .flatMap(tuples -> clientRequestRepository.save(tuples.getT1().update(request))
            .flatMap(clientRequest -> updateJobDetails(tuples.getT2(), clientRequest))
            .as(transactionalOperator::transactional)
            .doOnSuccess(jobDetails -> jobDetails.stream().forEach(quartzUtil::updateJob))
            .map(result -> tuples.getT1())
        )
        .map(clientRequest -> ClientResponseDTO.of(clientRequest));
  }

  private Mono<Tuple2<ClientRequest, List<JobDetail>>> findClientRequestAndJobDetail(ClientRequest clientRequest) {
    return jobDetailRepository.findByClientId(clientRequest.getId())
        .collectList()
        .map(jobDetails -> Tuples.of(clientRequest, jobDetails));
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
        .zipWith(clientRequestRepository.count())
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageRequest, tuple.getT2()))
        .map(pageResult -> CopyUtil.copy(pageResult.getContent(), ClientResponseDTO::new, composeResponse()));
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
  public Mono<Boolean> delete(String clientId) {
    return clientRequestRepository.deleteById(clientId)
        .zipWith(jobDetailRepository.findByClientId(clientId).collectList())
        .map(tuple -> tuple.getT2())
        .flatMap(this::findJobDetailAndJobExecutor)
        .doOnNext(tuple -> deleteJobExecutorAndJobDetail(tuple))
        .map(result -> Boolean.TRUE)
        .onErrorReturn(Boolean.FALSE);
  }

  private Mono<Tuple2<List<JobDetail>, List<JobExecutor>>> findJobDetailAndJobExecutor(List<JobDetail> jobDetails) {
    return jobExecutorRepository.findByJobIdIn(getJobDetailIds(jobDetails))
        .collectList()
        .map(jobExecutors -> Tuples.of(jobDetails, jobExecutors));
  }

  private void deleteJobExecutorAndJobDetail(Tuple2<List<JobDetail>, List<JobExecutor>> tuple) {
    jobExecutorRepository.deleteByIdIn(getExecutorIds(tuple.getT2()))
        .zipWith(jobDetailRepository.deleteByIdIn(getJobDetailIds(tuple.getT1())))
        .as(transactionalOperator::transactional)
        .doOnSuccess(result -> quartzUtil.removeJobs(tuple.getT2()))
        .subscribe();
  }

  private List<String> getExecutorIds(List<JobExecutor> jobExecutors) {
    return jobExecutors.stream()
        .map(JobExecutor::getId)
        .collect(Collectors.toList());
  }

  private List<String> getJobDetailIds(List<JobDetail> jobDetails) {
    return jobDetails.stream()
        .map(JobDetail::getId)
        .collect(Collectors.toList());
  }
}
