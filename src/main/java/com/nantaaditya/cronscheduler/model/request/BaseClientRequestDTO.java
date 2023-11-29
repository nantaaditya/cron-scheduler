package com.nantaaditya.cronscheduler.model.request;

import com.nantaaditya.cronscheduler.validation.HttpMethodMustValid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.URL;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseClientRequestDTO {
  @NotNull(message = "NotNull")
  @HttpMethodMustValid
  @Schema(type = "string", allowableValues = {"POST", "PUT", "GET", "DELETE"})
  private String httpMethod;
  @NotNull(message = "NotNull")
  @URL(message = "MustValid")
  @Size(max = 255, message = "TooLong")
  private String baseUrl;
  @NotNull(message = "NotNull")
  @Size(max = 255, message = "TooLong")
  private String apiPath;
  private Map<String, String> pathParams;
  private Map<String, String> queryParams;
  @NotNull(message = "NotNull")
  private Map<String, List<String>> headers;
  private Object payload;
}
