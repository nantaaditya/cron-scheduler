package com.nantaaditya.cronscheduler.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateJobExecutorRequestDTO extends BaseJobExecutorRequestDTO {
  @NotBlank(message = "NotBlank")
  private String jobName;
}
