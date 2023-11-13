package com.nantaaditya.cronscheduler.model.request;

import com.nantaaditya.cronscheduler.validation.ClientRequestMustExists;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateClientRequestDTO extends BaseClientRequestDTO {
  @NotBlank(message = "NotBlank")
  @ClientRequestMustExists
  private String clientName;
}
