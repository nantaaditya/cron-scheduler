package com.nantaaditya.cronscheduler.model.request;

import com.nantaaditya.cronscheduler.validation.ClientIdMustExists;
import com.nantaaditya.cronscheduler.validation.CronMustValid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BaseJobExecutorRequestDTO {
  @NotNull(message = "NotNull")
  @ClientIdMustExists
  private String clientId;
  @NotNull(message = "NotNull")
  @CronMustValid
  private String cronTriggerExpression;
  private boolean enable;
}
