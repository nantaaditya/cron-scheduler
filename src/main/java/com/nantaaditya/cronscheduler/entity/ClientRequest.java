package com.nantaaditya.cronscheduler.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nantaaditya.cronscheduler.model.request.CreateClientRequestDTO;
import com.nantaaditya.cronscheduler.model.request.UpdateClientRequestDTO;
import com.nantaaditya.cronscheduler.util.JsonHelper;
import io.r2dbc.postgresql.codec.Json;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.ObjectUtils;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "client_request")
public class ClientRequest extends BaseEntity {
  private String clientName;
  private String httpMethod;
  private String baseUrl;
  private String apiPath;
  private Json pathParams;
  private Json queryParams;
  private Json headers;
  private Json payload;

  public Map<String, String> getPathParams() {
    if (ObjectUtils.isEmpty(pathParams)) return null;

    return JsonHelper.fromJson(pathParams, new TypeReference<Map<String, String>>() {});
  }

  public Map<String, String> getQueryParams() {
    if (ObjectUtils.isEmpty(queryParams)) return null;

    return JsonHelper.fromJson(queryParams);
  }

  public Map<String, List<String>> getHeaders() {
    if (ObjectUtils.isEmpty(headers)) return null;

    return JsonHelper.fromJson(headers, new TypeReference<Map<String, List<String>>>() {});
  }

  public String getPayload() {
    if (ObjectUtils.isEmpty(payload)) return null;
    return payload.asString();
  }

  public String getApiPath() {
    if (getPathParams() == null) return apiPath;

    getPathParams().entrySet()
        .stream()
        .forEach(path -> apiPath.replace("{"+path.getKey()+"}", path.getValue()));

    return apiPath;
  }

  public static ClientRequest of(CreateClientRequestDTO request) {
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
}
