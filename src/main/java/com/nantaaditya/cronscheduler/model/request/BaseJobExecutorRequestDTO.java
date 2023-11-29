package com.nantaaditya.cronscheduler.model.request;

import com.nantaaditya.cronscheduler.validation.CronMustValid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseJobExecutorRequestDTO {
  @NotNull(message = "NotNull")
  private String clientId;
  @NotNull(message = "NotNull")
  @CronMustValid
  private String cronTriggerExpression;
  private boolean enable;
}
