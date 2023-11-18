package com.nantaaditya.cronscheduler.repository;

import com.nantaaditya.cronscheduler.entity.ClientRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
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
      j.id j_id, j.created_by j_created_by, j.created_date j_created_date, j.created_time j_created_time, j.modified_by j_modified_by, j.modified_date j_modified_date, j.modified_time j_modified_time, j.version j_version,
      j.client_id j_client_id, j.job_name j_job_name, j.job_group j_job_group, j.job_data j_job_data,
      e.id e_id, e.trigger_id e_trigger_id, e.active e_active
      FROM client_request c 
      JOIN job_detail j ON c.id = j.client_id
      JOIN job_executor e ON j.id = e.job_id
      """;

  public Mono<ClientRequest> findClientRequestAndJobDetailsByName(String clientName) {
    return dbClient.sql(String.format("%s WHERE c.client_name = :client_name", FIND_CLIENT_REQUEST_AND_JOB_DETAILS_SQL))
        .bind("client_name", clientName)
        .fetch()
        .all()
        .bufferUntilChanged()
        .map(ClientRequest::from)
        .singleOrEmpty();
  }

  public Mono<ClientRequest> findClientRequestAndJobDetailsById(String clientId) {
    return dbClient.sql(String.format("%s WHERE c.id = :id", FIND_CLIENT_REQUEST_AND_JOB_DETAILS_SQL))
        .bind("id", clientId)
        .fetch()
        .all()
        .bufferUntilChanged()
        .map(ClientRequest::from)
        .singleOrEmpty();
  }
}
