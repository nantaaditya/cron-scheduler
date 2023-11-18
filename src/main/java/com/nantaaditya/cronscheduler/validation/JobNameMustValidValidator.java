package com.nantaaditya.cronscheduler.validation;

import com.nantaaditya.cronscheduler.entity.JobExecutor;
import com.nantaaditya.cronscheduler.repository.JobExecutorRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobNameMustValidValidator implements ConstraintValidator<JobNameMustValid, String> {

  private JobNameMustValid validation;

  private final JobExecutorRepository jobExecutorRepository;

  @Override
  public void initialize(JobNameMustValid constraintAnnotation) {
    this.validation = constraintAnnotation;
  }

  @Override
  public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
    JobExecutor jobExecutor = jobExecutorRepository.findByJobName(s).block();
    return validation.create() ? jobExecutor == null : jobExecutor != null;
  }
}
