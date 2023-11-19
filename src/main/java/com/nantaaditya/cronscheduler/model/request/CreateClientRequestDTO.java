package com.nantaaditya.cronscheduler.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateClientRequestDTO extends BaseClientRequestDTO{
  @NotNull(message = "NotNull")
  private String clientName;
}
