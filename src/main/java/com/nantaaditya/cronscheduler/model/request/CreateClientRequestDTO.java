package com.nantaaditya.cronscheduler.model.request;

import com.nantaaditya.cronscheduler.validation.ClientRequestMustNotExists;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateClientRequestDTO extends BaseClientRequestDTO{
  @NotNull(message = "NotNull")
  @ClientRequestMustNotExists
  private String clientName;
}
