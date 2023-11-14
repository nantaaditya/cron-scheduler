package com.nantaaditya.cronscheduler.validation;

import com.nantaaditya.cronscheduler.repository.JobExecutorRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobExecutorMustExistsValidator implements ConstraintValidator<JobExecutorMustExists, String> {

  private final JobExecutorRepository jobExecutorRepository;

  @Override
  public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
    return jobExecutorRepository.findById(s).block() != null;
  }
}
