package com.nantaaditya.cronscheduler.model.request;

import com.nantaaditya.cronscheduler.validation.ClientRequestMustExists;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateClientRequestDTO extends BaseClientRequestDTO {
  @NotNull(message = "NotNull")
  @ClientRequestMustExists
  private String clientName;
}
