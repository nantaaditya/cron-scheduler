package com.nantaaditya.cronscheduler.model.request;

import com.nantaaditya.cronscheduler.validation.JobExecutorMustExists;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteJobExecutorRequestDTO {

  @NotNull(message = "NotNull")
  @JobExecutorMustExists
  private String jobExecutorId;
}
