package com.nantaaditya.cronscheduler.model.response;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
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
}
