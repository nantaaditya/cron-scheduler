package com.nantaaditya.cronscheduler.service;

import com.nantaaditya.cronscheduler.entity.ClientRequest;
import com.nantaaditya.cronscheduler.entity.JobExecutor;
import com.nantaaditya.cronscheduler.model.constant.InvalidParameterException;
import com.nantaaditya.cronscheduler.model.request.CreateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateJobExecutorRequestDTO;
import com.nantaaditya.cronscheduler.model.response.JobExecutorResponseDTO;
import com.nantaaditya.cronscheduler.repository.ClientRequestRepository;
import com.nantaaditya.cronscheduler.repository.CustomClientRequestRepository;
import com.nantaaditya.cronscheduler.repository.JobExecutorRepository;
import com.nantaaditya.cronscheduler.util.QuartzUtil;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class JobExecutorServiceImpl implements JobExecutorService {

  private final JobExecutorRepository jobExecutorRepository;
  private final ClientRequestRepository clientRequestRepository;
  private final CustomClientRequestRepository customClientRequestRepository;
  private final QuartzUtil quartzUtil;
  private final TransactionalOperator transactionalOperator;

  private static final String NOT_EXISTS = "NotExists";
  private static final String ALREADY_EXISTS = "AlreadyExists";
  private static final String INVALID_PARAMETER = "invalid parameter";

  @Override
  public Mono<JobExecutorResponseDTO> create(CreateJobExecutorRequestDTO request) {
    return Mono.zip(
        clientRequestRepository.existsById(request.getClientId()),
        jobExecutorRepository.existsByJobName(request.getJobName()),
        Tuples::of
    )
      .handle((tuples, sink) -> {
        if (tuples.getT1().booleanValue() && !tuples.getT2().booleanValue()) {
          sink.next(request);
        } else {
          Map<String, List<String>> errors = new HashMap<>();
          if (!tuples.getT1().booleanValue())
            errors.put("clientId", List.of(NOT_EXISTS));
          if (!tuples.getT2().booleanValue())
            errors.put("jobName", List.of(ALREADY_EXISTS));

          sink.error(new InvalidParameterException(errors, INVALID_PARAMETER));
        }
      })
      .flatMap(r -> clientRequestRepository.findById(request.getClientId()))
      .flatMap(tuples -> saveJobExecutor(tuples, request))
      .as(transactionalOperator::transactional)
      .doOnNext(tuples ->
        quartzUtil.createJob(tuples.getT2())
      )
      .map(tuples -> JobExecutorResponseDTO.of(tuples.getT2(), tuples.getT1()));
  }

  private Mono<Tuple2<ClientRequest, JobExecutor>> saveJobExecutor(ClientRequest clientRequest,
      CreateJobExecutorRequestDTO request) {
    return jobExecutorRepository.save(JobExecutor.create(request, clientRequest))
        .map(jobExecutor -> Tuples.of(clientRequest, jobExecutor));
  }

  @Override
  public Mono<JobExecutorResponseDTO> update(UpdateJobExecutorRequestDTO request) {
    return Mono.zip(
        clientRequestRepository.existsById(request.getClientId()),
        jobExecutorRepository.existsById(request.getJobExecutorId())
    )
      .handle((tuples, sink) -> {
        if (tuples.getT1().booleanValue() && tuples.getT2().booleanValue()) {
          sink.next(request);
        } else {
          Map<String, List<String>> errors = new HashMap<>();
          if (!tuples.getT1().booleanValue())
            errors.put("clientId", List.of(NOT_EXISTS));
          if (!tuples.getT2().booleanValue())
            errors.put("jobExecutorId", List.of(NOT_EXISTS));

          sink.error(new InvalidParameterException(errors, INVALID_PARAMETER));
        }
      })
      .flatMap(r -> Mono.zip(
          jobExecutorRepository.findById(request.getJobExecutorId()),
          clientRequestRepository.findById(request.getClientId()),
          Tuples::of
      ))
     .flatMap(tuples -> update(tuples, request))
     .doOnNext(tuples -> quartzUtil.updateJob(tuples.getT1()))
     .map(result -> JobExecutorResponseDTO.of(result.getT1(), result.getT2()));
  }

  private Mono<Tuple2<JobExecutor, ClientRequest>> update(Tuple2<JobExecutor, ClientRequest> tuples,
      UpdateJobExecutorRequestDTO request) {
    JobExecutor jobExecutor = tuples.getT1();
    ClientRequest clientRequest = tuples.getT2();

    jobExecutor.setActive(request.isEnable());
    jobExecutor.setTriggerCron(request.getCronTriggerExpression());
    jobExecutor.setClientId(clientRequest.getId());
    jobExecutor.setTriggerCron(jobExecutor.getTriggerCron());
    jobExecutor.putJobDataMap(clientRequest);

    return jobExecutorRepository.save(jobExecutor)
        .map(result -> Tuples.of(result, clientRequest));
  }

  @Override
  public Mono<List<JobExecutorResponseDTO>> findAll(int page, int size) {
    return jobExecutorRepository.findAllBy(PageRequest.of(page, size))
        .collectList()
        .flatMap(jobExecutors -> clientRequestRepository.findByIdIn(getClientIds(jobExecutors))
            .collectList()
            .map(clientRequests -> Tuples.of(jobExecutors, clientRequests))
        )
        .map(tuples -> toResponse(tuples.getT1(), tuples.getT2()))
        .switchIfEmpty(Mono.just(List.of()));
  }

  private List<JobExecutorResponseDTO> toResponse(List<JobExecutor> jobExecutors, List<ClientRequest> clientRequests) {
    List<JobExecutorResponseDTO> responses = new LinkedList<>();

    for (JobExecutor jobExecutor : jobExecutors) {
      responses.add(JobExecutorResponseDTO.of(
          jobExecutor,
          getClientRequest(clientRequests, jobExecutor)
      ));
    }

    return responses;
  }

  private ClientRequest getClientRequest(List<ClientRequest> clientRequests, JobExecutor jobExecutor) {
    return clientRequests.stream()
        .filter(clientRequest -> clientRequest.getId().equals(jobExecutor.getClientId()))
        .findFirst()
        .orElse(null);
  }

  private List<String> getClientIds(List<JobExecutor> jobExecutors) {
    return jobExecutors.stream()
        .map(JobExecutor::getClientId)
        .toList();
  }

  @Override
  public Mono<JobExecutorResponseDTO> findById(String id) {
    return jobExecutorRepository.existsById(id)
        .handle((exists, sink) -> {
          if (!exists.booleanValue()) {
            sink.error(new InvalidParameterException(Map.of("id", List.of(NOT_EXISTS)), INVALID_PARAMETER));
          } else {
            sink.next(id);
          }
        })
        .flatMap(r -> customClientRequestRepository.findClientRequestAndJobDetailsByExecutorId(id))
        .map(clientRequest -> JobExecutorResponseDTO.of(
            getJobExecutor(clientRequest.getJobExecutors(), id),
            clientRequest
            )
        );
  }

  private JobExecutor getJobExecutor(List<JobExecutor> jobExecutors, String jobExecutorId) {
    return jobExecutors.stream()
        .filter(jobExecutor -> jobExecutorId.equals(jobExecutor.getId()))
        .findFirst()
        .orElse(null);
  }

  @Override
  public Mono<Boolean> deleteById(String id) {
    return jobExecutorRepository.existsById(id)
        .handle((exists, sink) -> {
          if (!exists.booleanValue()) {
            sink.error(new InvalidParameterException(Map.of("id", List.of(NOT_EXISTS)), INVALID_PARAMETER));
          } else {
            sink.next(id);
          }
        })
        .flatMap(r -> jobExecutorRepository.deleteById(id))
        .doOnSuccess(result -> quartzUtil.removeJob(id))
        .map(result -> Boolean.TRUE);
  }

  @Override
  public Mono<JobExecutorResponseDTO> toggle(String id, boolean enable) {
    return jobExecutorRepository.existsById(id)
        .handle((exists, sink) -> {
          if (!exists.booleanValue()) {
            sink.error(new InvalidParameterException(Map.of("id", List.of(NOT_EXISTS)), INVALID_PARAMETER));
          } else {
            sink.next(id);
          }
        })
        .flatMap(r -> customClientRequestRepository.findClientRequestAndJobDetailsByExecutorId(id))
        .map(clientRequest -> Tuples.of(
            clientRequest,
            getJobExecutor(clientRequest.getJobExecutors(), id)
            )
        )
        .flatMap(tuples -> {
          tuples.getT2().setActive(enable);
          return jobExecutorRepository.save(tuples.getT2())
              .map(jobExecutor -> Tuples.of(tuples.getT1(), jobExecutor));
        })
        .doOnNext(tuples -> quartzUtil.updateJob(tuples.getT2()))
        .map(tuples -> JobExecutorResponseDTO.of(tuples.getT2(), tuples.getT1()));
  }

  @Override
  public Mono<Boolean> run(String id) {
    return jobExecutorRepository.existsById(id)
        .handle((exists, sink) -> {
          if (!exists.booleanValue()) {
            sink.error(new InvalidParameterException(Map.of("id", List.of(NOT_EXISTS)), INVALID_PARAMETER));
          } else {
            sink.next(id);
          }
        })
        .flatMap(r -> customClientRequestRepository.findClientRequestAndJobDetailsByExecutorId(id))
        .map(clientRequest -> Tuples.of(
                clientRequest,
                getJobExecutor(clientRequest.getJobExecutors(), id)
            )
        )
        .doOnNext(tuples -> quartzUtil.runNow(tuples.getT2()))
        .map(result -> Boolean.TRUE);
  }
}
