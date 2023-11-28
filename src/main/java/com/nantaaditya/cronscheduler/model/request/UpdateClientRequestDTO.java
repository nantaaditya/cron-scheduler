package com.nantaaditya.cronscheduler.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateClientRequestDTO extends BaseClientRequestDTO {
  @NotNull(message = "NotNull")
  @Size(max = 255, message = "TooLong")
  private String clientName;
}
