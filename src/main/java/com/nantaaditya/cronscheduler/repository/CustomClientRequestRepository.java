package com.nantaaditya.cronscheduler.repository;

import com.nantaaditya.cronscheduler.entity.ClientRequest;
import com.nantaaditya.cronscheduler.entity.JobExecutor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomClientRequestRepository {

  private final DatabaseClient dbClient;

  private static final String FIND_CLIENT_REQUEST_AND_JOB_DETAILS_SQL = """
      SELECT 
      c.id c_id, c.created_by c_created_by, c.created_date c_created_date, c.created_time c_created_time, c.modified_by c_modified_by, c.modified_date c_modified_date, c.modified_time c_modified_time, c.version c_version,
      c.client_name c_client_name, c.http_method c_http_method, c.base_url c_base_url, c.api_path c_api_path, c.path_params c_path_params, c.query_params c_query_params, c.headers c_headers, c.payload c_payload,
      e.id e.id, e.created_by e.created_by, e.created_date e.created_date, e.created_time e.created_time, e.modified_by e.modified_by, e.modified_date e.modified_date, e.modified_time e.modified_time, e.version e.version,
      e.client_id e.client_id, e.job_name e.job_name, e.job_group e.job_group, e.job_data e.job_data, e.trigger_cron e_trigger_cron, e.active e_active
      FROM client_request c 
      JOIN job_executor e ON c.id = e.client_id
      """;

  public Mono<ClientRequest> findClientRequestAndJobExecutorsByName(String clientName) {
    return dbClient.sql(String.format("%s WHERE c.client_name = :client_name", FIND_CLIENT_REQUEST_AND_JOB_DETAILS_SQL))
        .bind("client_name", clientName)
        .fetch()
        .all()
        .bufferUntilChanged()
        .map(ClientRequest::from)
        .singleOrEmpty();
  }

  public Mono<ClientRequest> findClientRequestAndJobExecutorsById(String clientId) {
    return dbClient.sql(String.format("%s WHERE c.id = :id", FIND_CLIENT_REQUEST_AND_JOB_DETAILS_SQL))
        .bind("id", clientId)
        .fetch()
        .all()
        .bufferUntilChanged()
        .map(ClientRequest::from)
        .singleOrEmpty();
  }

  public Mono<ClientRequest> findClientRequestAndJobDetailsByExecutorId(String jobExecutorId) {
    return dbClient.sql(String.format("%s WHERE e.id = :id", FIND_CLIENT_REQUEST_AND_JOB_DETAILS_SQL))
        .bind("id", jobExecutorId)
        .fetch()
        .all()
        .bufferUntilChanged()
        .map(ClientRequest::from)
        .singleOrEmpty();
  }

  public Mono<List<ClientRequest>> findClientRequestAndJobDetailsByExecutorId(List<JobExecutor> jobExecutors) {
    return Flux.fromIterable(jobExecutors)
        .flatMap(jobExecutor -> findClientRequestAndJobDetailsByExecutorId(jobExecutor.getId()))
        .collectList();
  }

}
