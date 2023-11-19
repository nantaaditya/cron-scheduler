package com.nantaaditya.cronscheduler.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class Response<T> {
  private boolean success;
  private T data;
  private Map<String, List<String>> errors;

  public static <T> Response<T> ok(T data) {
    Response<T> response = new Response<>();
    response.setSuccess(true);
    response.setData(data);
    return response;
  }

  public static <T> Response<T> error(Map<String, List<String>> errors) {
    Response<T> response = new Response<>();
    response.setSuccess(false);
    response.setErrors(errors);
    return response;
  }
}
