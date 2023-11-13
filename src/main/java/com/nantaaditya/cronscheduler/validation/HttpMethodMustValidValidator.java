package com.nantaaditya.cronscheduler.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.http.HttpMethod;

public class HttpMethodMustValidValidator implements ConstraintValidator<HttpMethodMustValid, String> {

  @Override
  public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
    return HttpMethod.valueOf(s) != null;
  }
}
