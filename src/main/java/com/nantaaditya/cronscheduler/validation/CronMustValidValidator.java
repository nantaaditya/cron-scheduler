package com.nantaaditya.cronscheduler.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.quartz.CronScheduleBuilder;

public class CronMustValidValidator implements ConstraintValidator<CronMustValid, String> {

  @Override
  public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
    try {
      CronScheduleBuilder.cronSchedule(s);
      return true;
    } catch (RuntimeException ex) {
      return false;
    }
  }
}
