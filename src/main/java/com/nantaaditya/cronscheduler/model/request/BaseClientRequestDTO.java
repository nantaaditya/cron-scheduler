package com.nantaaditya.cronscheduler.model.request;

import com.nantaaditya.cronscheduler.validation.HttpMethodMustValid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@NoArgsConstructor
public class BaseClientRequestDTO {
  @NotBlank(message = "NotBlank")
  @HttpMethodMustValid
  private String httpMethod;
  @NotBlank(message = "NotBlank")
  @URL(message = "MustValid")
  private String baseUrl;
  @NotBlank(message = "NotBlank")
  private String apiPath;
  private Map<String, String> pathParams;
  private Map<String, String> queryParams;
  @NotNull(message = "NotNull")
  private Map<String, List<String>> headers;
  private Object payload;
}
