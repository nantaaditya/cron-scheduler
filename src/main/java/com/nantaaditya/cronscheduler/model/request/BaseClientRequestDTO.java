package com.nantaaditya.cronscheduler.model.request;

import com.nantaaditya.cronscheduler.validation.HttpMethodMustValid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@NoArgsConstructor
public class BaseClientRequestDTO {
  @NotNull(message = "NotNull")
  @HttpMethodMustValid
  private String httpMethod;
  @NotNull(message = "NotNull")
  @URL(message = "MustValid")
  private String baseUrl;
  @NotNull(message = "NotNull")
  private String apiPath;
  private Map<String, String> pathParams;
  private Map<String, String> queryParams;
  @NotNull(message = "NotNull")
  private Map<String, List<String>> headers;
  private Object payload;
}
