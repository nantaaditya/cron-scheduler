package com.nantaaditya.cronscheduler.model.request;

import com.nantaaditya.cronscheduler.validation.JobExecutorMustExists;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateJobExecutorRequestDTO extends BaseJobExecutorRequestDTO {

  @NotNull(message = "NotNull")
  @JobExecutorMustExists
  private String jobExecutorId;
}
