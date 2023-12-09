package com.nantaaditya.cronscheduler.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.nantaaditya.cronscheduler.model.constant.InvalidParameterException;
import com.nantaaditya.cronscheduler.model.request.CreateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateClientRequestDTO;
import com.nantaaditya.cronscheduler.util.JsonHelper;
import io.r2dbc.postgresql.codec.Json;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.ObjectUtils;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "client_request")
@JsonInclude(Include.NON_NULL)
@EqualsAndHashCode(callSuper = true)
public class ClientRequest extends BaseEntity {
  private String clientName;
  private String httpMethod;
  private String baseUrl;
  private String apiPath;
  private Json pathParams;
  private Json queryParams;
  private Json headers;
  private Json payload;
  @Transient
  @JsonIgnore
  private List<JobExecutor> jobExecutors;

  public Map<String, String> getPathParams() {
    if (ObjectUtils.isEmpty(pathParams)) return null;

    return JsonHelper.fromJson(pathParams, new TypeReference<Map<String, String>>() {});
  }

  public Map<String, String> getQueryParamMap() {
    if (ObjectUtils.isEmpty(queryParams)) return null;

    return JsonHelper.fromJson(queryParams);
  }

  public Map<String, List<String>> getHeaders() {
    if (ObjectUtils.isEmpty(headers)) return null;

    return JsonHelper.fromJson(headers, new TypeReference<Map<String, List<String>>>() {});
  }

  public String getPayloadString() {
    if (ObjectUtils.isEmpty(payload)) return null;
    return payload.asString();
  }

  public String getFullApiPath() {
    if (getPathParams() == null) return apiPath;

    getPathParams().entrySet()
        .stream()
        .forEach(path -> apiPath.replace("{"+path.getKey()+"}", path.getValue()));

    return apiPath;
  }

  public JobExecutor getJobExecutor(String jobExecutorId) {
    return getJobExecutors()
        .stream()
        .filter(je -> je.getId().equals(jobExecutorId))
        .findFirst()
        .orElseThrow(() -> new InvalidParameterException(
            Map.of("jobExecutorId", List.of("NotExists")), "invalid parameter"
            )
        );
  }

  public ClientRequest update(UpdateClientRequestDTO request) {
    setHttpMethod(request.getHttpMethod());
    setBaseUrl(request.getBaseUrl());
    setApiPath(request.getApiPath());
    setPathParams(JsonHelper.toJson(request.getPathParams()));
    setQueryParams(JsonHelper.toJson(request.getQueryParams()));
    setHeaders(JsonHelper.toJson(request.getHeaders()));
    setPayload(JsonHelper.toJson(request.getPayload()));
    return this;
  }

  public static ClientRequest create(CreateClientRequestDTO request) {
    return ClientRequest.builder()
        .id(BaseEntity.generateId())
        .createdBy(BaseEntity.AUDITOR)
        .modifiedBy(BaseEntity.AUDITOR)
        .clientName(request.getClientName())
        .httpMethod(request.getHttpMethod())
        .baseUrl(request.getBaseUrl())
        .apiPath(request.getApiPath())
        .pathParams(JsonHelper.toJson(request.getPathParams()))
        .queryParams(JsonHelper.toJson(request.getQueryParams()))
        .headers(JsonHelper.toJson(request.getHeaders()))
        .payload(JsonHelper.toJson(request.getPayload()))
        .build();
  }

  public static ClientRequest from(List<Map<String, Object>> rows) {
    return ClientRequest.builder()
        .id((String) rows.get(0).get("c_id"))
        .createdBy((String) rows.get(0).get("c_created_by"))
        .createdDate((LocalDate) rows.get(0).get("c_created_date"))
        .createdTime((LocalTime) rows.get(0).get("c_created_time"))
        .modifiedBy((String) rows.get(0).get("c_modified_by"))
        .modifiedDate((LocalDate) rows.get(0).get("c_modified_date"))
        .modifiedTime((LocalTime) rows.get(0).get("c_modified_time"))
        .version((Long) rows.get(0).get("c_version"))
        .clientName((String) rows.get(0).get("c_client_name"))
        .httpMethod((String) rows.get(0).get("c_http_method"))
        .baseUrl((String) rows.get(0).get("c_base_url"))
        .apiPath((String) rows.get(0).get("c_api_path"))
        .pathParams((Json) rows.get(0).get("c_path_params"))
        .queryParams((Json) rows.get(0).get("c_query_params"))
        .headers((Json) rows.get(0).get("c_headers"))
        .payload((Json) rows.get(0).get("c_payload"))
        .jobExecutors(JobExecutor.from(rows))
        .build();
  }
}
