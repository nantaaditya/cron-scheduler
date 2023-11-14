package com.nantaaditya.cronscheduler.validation;

import com.nantaaditya.cronscheduler.entity.JobDetail;
import com.nantaaditya.cronscheduler.repository.JobDetailRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobNameMustValidValidator implements ConstraintValidator<JobNameMustValid, String> {

  private JobNameMustValid validation;

  private final JobDetailRepository jobDetailRepository;

  @Override
  public void initialize(JobNameMustValid constraintAnnotation) {
    this.validation = constraintAnnotation;
  }

  @Override
  public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
    JobDetail jobDetail = jobDetailRepository.findByJobName(s).block();
    return validation.create() ? jobDetail == null : jobDetail != null;
  }
}
