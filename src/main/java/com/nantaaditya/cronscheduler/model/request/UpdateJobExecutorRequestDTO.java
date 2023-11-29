package com.nantaaditya.cronscheduler.model.request;

import jakarta.validation.constraints.NotNull;
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
public class UpdateJobExecutorRequestDTO extends BaseJobExecutorRequestDTO {

  @NotNull(message = "NotNull")
  private String jobExecutorId;
}
