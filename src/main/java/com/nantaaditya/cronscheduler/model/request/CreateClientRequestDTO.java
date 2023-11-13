package com.nantaaditya.cronscheduler.model.request;

import com.nantaaditya.cronscheduler.validation.ClientRequestMustNotExists;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateClientRequestDTO extends BaseClientRequestDTO{
  @NotBlank(message = "NotBlank")
  @ClientRequestMustNotExists
  private String clientName;
}
