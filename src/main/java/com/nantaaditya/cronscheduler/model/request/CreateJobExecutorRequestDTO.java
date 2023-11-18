package com.nantaaditya.cronscheduler.model.request;

import com.nantaaditya.cronscheduler.validation.JobNameMustValid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateJobExecutorRequestDTO extends BaseJobExecutorRequestDTO {
  @NotBlank(message = "NotBlank")
  @JobNameMustValid(create = true, message = "AlreadyExists")
  private String jobName;
}
