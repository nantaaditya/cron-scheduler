package com.nantaaditya.cronscheduler.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateClientRequestDTO extends BaseClientRequestDTO{
  @NotNull(message = "NotNull")
  @Size(max = 255, message = "TooLong")
  private String clientName;
}
