package com.nantaaditya.cronscheduler.validation;

import com.nantaaditya.cronscheduler.entity.JobTrigger;
import com.nantaaditya.cronscheduler.repository.JobTriggerRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TriggerNameMustValidValidator implements ConstraintValidator<TriggerNameMustValid, String> {

  private TriggerNameMustValid validation;

  private final JobTriggerRepository jobTriggerRepository;


  @Override
  public void initialize(TriggerNameMustValid constraintAnnotation) {
    this.validation = constraintAnnotation;
  }

  @Override
  public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
    JobTrigger jobTrigger = jobTriggerRepository.findByTriggerName(s).block();
    return validation.create() ? jobTrigger == null : jobTrigger != null;
  }
}
