package com.nantaaditya.cronscheduler.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nantaaditya.cronscheduler.entity.JobHistory;
import com.nantaaditya.cronscheduler.entity.JobHistoryDetail;
import com.nantaaditya.cronscheduler.model.response.JobHistoryResponseDTO;
import com.nantaaditya.cronscheduler.repository.CustomJobHistoryRepository;
import com.nantaaditya.cronscheduler.repository.JobHistoryDetailRepository;
import com.nantaaditya.cronscheduler.repository.JobHistoryRepository;
import com.nantaaditya.cronscheduler.util.JsonHelper;
import io.r2dbc.postgresql.codec.Json;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobHistoryServiceImpl implements JobHistoryService {

  private final CustomJobHistoryRepository customJobHistoryRepository;
  private final JobHistoryRepository jobHistoryRepository;
  private final JobHistoryDetailRepository jobHistoryDetailRepository;

  @Override
  public Mono<List<JobHistoryResponseDTO>> findAll(int page, int size) {
    return customJobHistoryRepository.findAll(page, size)
        .map(this::toResponses)
        .switchIfEmpty(Mono.just(Collections.emptyList()));
  }

  @Override
  public Mono<Boolean> removeObsoleteHistory(int retentionDays) {
    return jobHistoryDetailRepository.deleteByCreatedDateBefore(LocalDate.now().minusDays(retentionDays))
        .doOnNext(jobHistoryDetailRecords -> log.info("#JobHistory - deleted job history detail {} record", jobHistoryDetailRecords))
        .flatMap(jobHistoryDetailRecords -> jobHistoryRepository.deleteByCreatedDateBefore(LocalDate.now().minusDays(retentionDays)))
        .doOnNext(jobHistoryRecords -> log.info("#JobHistory - deleted job history {} record", jobHistoryRecords))
        .thenReturn(Boolean.TRUE);
  }

  private List<JobHistoryResponseDTO> toResponses(List<JobHistory> jobHistories) {
    return jobHistories.stream()
        .map(this::toResponse)
        .collect(Collectors.toCollection(LinkedList::new));
  }

  private JobHistoryResponseDTO toResponse(JobHistory jobHistory) {
    JobHistoryDetail jobHistoryDetail = jobHistory.getJobHistoryDetail();

    return JobHistoryResponseDTO.builder()
        .id(jobHistory.getId())
        .jobExecutorId(jobHistory.getJobExecutorId())
        .executedDate(jobHistory.getExecutedDate())
        .executedTime(jobHistory.getExecutedTime())
        .triggerCron(jobHistory.getTriggerCron())
        .status(jobHistory.getStatus())
        .clientRequest(JsonHelper.fromJson(
            jobHistoryDetail.getClientRequest(),
            new TypeReference<Map<String, Object>>() {})
        )
        .result(toResult(jobHistoryDetail.getResultDetail()))
        .build();
  }

  @SneakyThrows
  private Object toResult(Json resultDetail) {
    Map<String, Object> resultMap = JsonHelper.fromJson(resultDetail.asString(), Map.class);
    return resultMap.get("clientResponse");
  }
}
