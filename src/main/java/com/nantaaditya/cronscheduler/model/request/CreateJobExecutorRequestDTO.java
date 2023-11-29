package com.nantaaditya.cronscheduler.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateJobExecutorRequestDTO extends BaseJobExecutorRequestDTO {
  @NotBlank(message = "NotBlank")
  private String jobName;
}
