package com.nantaaditya.cronscheduler.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.nantaaditya.cronscheduler.entity.ClientRequest;
import com.nantaaditya.cronscheduler.util.CopyUtil;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class ClientResponseDTO {
  private String id;
  private String clientName;
  private String httpMethod;
  private String baseUrl;
  private String apiPath;
  private Map<String, String> pathParams;
  private Map<String, String> queryParams;
  private Map<String, List<String>> headers;
  private int timeoutInMillis;
  private Object payload;

  public static ClientResponseDTO of(ClientRequest clientRequest) {
    ClientResponseDTO response = CopyUtil.copy(clientRequest, ClientResponseDTO::new);
    response.setPathParams(clientRequest.getPathParams());
    response.setQueryParams(clientRequest.getQueryParamMap());
    response.setHeaders(clientRequest.getHeaders());
    response.setPayload(clientRequest.getPayloadString());
    response.setTimeoutInMillis(clientRequest.getTimeoutInMillis());
    return response;
  }
}
