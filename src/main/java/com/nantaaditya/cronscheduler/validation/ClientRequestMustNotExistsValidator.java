package com.nantaaditya.cronscheduler.validation;

import com.nantaaditya.cronscheduler.repository.ClientRequestRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientRequestMustNotExistsValidator implements ConstraintValidator<ClientRequestMustNotExists, String> {

  private final ClientRequestRepository clientRequestRepository;

  @Override
  public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
    return !clientRequestRepository.existsByClientName(s).block();
  }
}
