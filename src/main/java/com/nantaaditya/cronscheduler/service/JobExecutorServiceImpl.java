package com.nantaaditya.cronscheduler.service;

import com.nantaaditya.cronscheduler.entity.ClientRequest;
import com.nantaaditya.cronscheduler.entity.JobDetail;
import com.nantaaditya.cronscheduler.entity.JobExecutor;
import com.nantaaditya.cronscheduler.entity.JobTrigger;
import com.nantaaditya.cronscheduler.model.request.CreateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.model.response.JobExecutorResponseDTO;
import com.nantaaditya.cronscheduler.repository.ClientRequestRepository;
import com.nantaaditya.cronscheduler.repository.JobDetailRepository;
import com.nantaaditya.cronscheduler.repository.JobExecutorRepository;
import com.nantaaditya.cronscheduler.repository.JobTriggerRepository;
import com.nantaaditya.cronscheduler.util.QuartzUtil;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutorServiceImpl implements JobExecutorService {

  private final JobExecutorRepository jobExecutorRepository;
  private final JobDetailRepository jobDetailRepository;
  private final JobTriggerRepository jobTriggerRepository;
  private final ClientRequestRepository clientRequestRepository;
  private final QuartzUtil quartzUtil;
  private final TransactionalOperator transactionalOperator;

  @Override
  public Mono<JobExecutorResponseDTO> create(CreateJobExecutorRequestDTO request) {
    return Mono.zip(
        clientRequestRepository.findById(request.getClientId()),
        jobTriggerRepository.save(JobTrigger.of(request)),
        Tuples::of
    )
        .flatMap(tuples -> saveJobDetailAndExecutor(tuples, request))
        .flatMap(this::updateJobDetail)
        .as(transactionalOperator::transactional)
        .doOnNext(tuples ->
          quartzUtil.createJob(tuples.getT3(), request.isEnable())
        )
        .map(tuples -> JobExecutorResponseDTO.of(
            tuples.getT4().getId(), tuples.getT1(), tuples.getT3(), tuples.getT2(), request.isEnable()
            )
        );
  }

  private Mono<Tuple4<ClientRequest, JobTrigger, JobDetail, JobExecutor>> saveJobDetailAndExecutor(
      Tuple2<ClientRequest, JobTrigger> tuples, CreateJobExecutorRequestDTO request) {
    return jobDetailRepository.save(JobDetail.of(request))
        .flatMap(jobDetail -> jobExecutorRepository.save(
                    JobExecutor.of(jobDetail.getId(), tuples.getT2().getId(), request.isEnable())
                )
                .map(jobExecutor -> Tuples.of(tuples.getT1(), tuples.getT2(), jobDetail, jobExecutor))
        );
  }

  private Mono<Tuple4<ClientRequest, JobTrigger, JobDetail, JobExecutor>> updateJobDetail(
      Tuple4<ClientRequest, JobTrigger, JobDetail, JobExecutor> tuples) {
    ClientRequest clientRequest = tuples.getT1();
    JobTrigger jobTrigger = tuples.getT2();
    JobDetail jobDetail = tuples.getT3();
    JobExecutor jobExecutor = tuples.getT4();

    jobDetail.putJobDataMap(jobExecutor.getId(), clientRequest, jobTrigger);

    return jobDetailRepository.save(jobDetail)
        .map(result -> Tuples.of(clientRequest, jobTrigger, result, jobExecutor));
  }

  @Override
  public Mono<JobExecutorResponseDTO> update(UpdateJobExecutorRequestDTO request) {
    return Mono.zip(
        jobExecutorRepository.findById(request.getJobExecutorId()),
        clientRequestRepository.findById(request.getClientId()),
        Tuples::of
    )
        .flatMap(this::findJobDetailAndTrigger)
        .flatMap(tuples -> update(tuples, request))
        .doOnNext(tuples ->
            quartzUtil.updateJob(tuples.getT3(), request.isEnable())
        )
        .map(result -> JobExecutorResponseDTO.of(
            result.getT1().getId(), result.getT2(), result.getT3(), result.getT4(), request.isEnable()
        ));
  }

  private Mono<Tuple4<JobExecutor, ClientRequest, JobDetail, JobTrigger>> findJobDetailAndTrigger(Tuple2<JobExecutor, ClientRequest> tuples) {
    return Mono.zip(
        jobDetailRepository.findById(tuples.getT1().getJobId()),
        jobTriggerRepository.findById(tuples.getT1().getTriggerId()),
        (JobDetail jobDetail, JobTrigger jobTrigger) -> Tuples.of(tuples.getT1(), tuples.getT2(), jobDetail, jobTrigger)
    );
  }

  private Mono<Tuple4<JobExecutor, ClientRequest, JobDetail, JobTrigger>> update(
      Tuple4<JobExecutor, ClientRequest, JobDetail, JobTrigger> tuples, UpdateJobExecutorRequestDTO request) {
    JobExecutor jobExecutor = tuples.getT1();
    ClientRequest clientRequest = tuples.getT2();
    JobDetail jobDetail = tuples.getT3();
    JobTrigger jobTrigger = tuples.getT4();

    jobExecutor.setActive(request.isEnable());

    jobTrigger.update(request);

    jobDetail.setClientId(clientRequest.getId());
    jobDetail.updateClientRequest(clientRequest);
    jobDetail.updateJobTrigger(jobTrigger);

    return Mono.zip(
        jobExecutorRepository.save(jobExecutor),
        jobTriggerRepository.save(jobTrigger),
        jobDetailRepository.save(jobDetail)
    )
        .as(transactionalOperator::transactional)
        .map(result -> Tuples.of(result.getT1(), clientRequest, result.getT3(), result.getT2()));
  }

  @Override
  public Mono<List<JobExecutorResponseDTO>> findAll(int page, int size) {
    return jobExecutorRepository.findAllBy(PageRequest.of(page, size))
        .collectList()
        .flatMap(jobExecutors -> Mono.zip(
           findJobDetailsAndClient(jobExecutors),
           jobTriggerRepository.findByIdIn(getFromJobExecutors(jobExecutors, JobExecutor::getTriggerId))
               .collectList(),
           (Tuple2<List<JobDetail>, List<ClientRequest>> tuples, List<JobTrigger> jobTriggers) ->
                Tuples.of(jobExecutors, tuples.getT1(), tuples.getT2(), jobTriggers)
        ))
        .map(tuples -> toResponse(tuples.getT1(), tuples.getT2(), tuples.getT3(), tuples.getT4()));
  }

  private Mono<Tuple2<List<JobDetail>, List<ClientRequest>>> findJobDetailsAndClient(List<JobExecutor> jobExecutors) {
    return jobDetailRepository.findByIdIn(getFromJobExecutors(jobExecutors, JobExecutor::getJobId))
        .collectList()
        .flatMap(jobDetails -> clientRequestRepository.findByIdIn(getClientIds(jobDetails))
            .collectList()
            .map(clientRequests -> Tuples.of(jobDetails, clientRequests))
        );
  }

  private List<JobExecutorResponseDTO> toResponse(List<JobExecutor> jobExecutors, List<JobDetail> jobDetails,
      List<ClientRequest> clientRequests, List<JobTrigger> jobTriggers) {
    List<JobExecutorResponseDTO> responses = new LinkedList<>();

    for (JobExecutor jobExecutor : jobExecutors) {
      JobDetail jobDetail = getJobDetail(jobDetails, jobExecutor);

      responses.add(JobExecutorResponseDTO.of(
          jobExecutor.getId(),
          getClientRequest(clientRequests, jobDetail),
          jobDetail,
          getJobTrigger(jobTriggers, jobExecutor),
          jobExecutor.isActive()
      ));
    }

    return responses;
  }

  private JobTrigger getJobTrigger(List<JobTrigger> jobTriggers, JobExecutor jobExecutor) {
    return jobTriggers.stream()
        .filter(jobTrigger -> jobTrigger.getId().equals(jobExecutor.getTriggerId()))
        .findFirst()
        .orElse(null);
  }

  private ClientRequest getClientRequest(List<ClientRequest> clientRequests, JobDetail jobDetail) {
    return clientRequests.stream()
        .filter(clientRequest -> clientRequest.getId().equals(jobDetail.getClientId()))
        .findFirst()
        .orElse(null);
  }

  private JobDetail getJobDetail(List<JobDetail> jobDetails, JobExecutor jobExecutor) {
    return jobDetails.stream()
        .filter(jobDetail -> jobDetail.getId().equals(jobExecutor.getJobId()))
        .findFirst()
        .orElse(null);
  }

  private List<String> getFromJobExecutors(List<JobExecutor> jobExecutors, Function<JobExecutor, String> function) {
    return jobExecutors.stream()
        .map(function)
        .collect(Collectors.toList());
  }

  private List<String> getClientIds(List<JobDetail> jobDetails) {
    return jobDetails.stream()
        .map(JobDetail::getClientId)
        .collect(Collectors.toList());
  }

  @Override
  public Mono<JobExecutorResponseDTO> findById(String jobExecutorId) {
    return null;
  }

  @Override
  public Mono<Boolean> deleteById(String jobExecutorId) {
    return null;
  }

  @Override
  public Mono<JobExecutorResponseDTO> toggle(String jobExecutorId, boolean enable) {
    return null;
  }
}
