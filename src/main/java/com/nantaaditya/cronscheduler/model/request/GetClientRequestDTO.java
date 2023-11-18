package com.nantaaditya.cronscheduler.model.request;

import com.nantaaditya.cronscheduler.validation.ClientIdMustExists;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetClientRequestDTO {

  @NotNull(message = "NotNull")
  @ClientIdMustExists
  private String clientId;
}
