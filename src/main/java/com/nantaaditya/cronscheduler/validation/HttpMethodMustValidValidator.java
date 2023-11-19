package com.nantaaditya.cronscheduler.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

public class HttpMethodMustValidValidator implements ConstraintValidator<HttpMethodMustValid, String> {

  @Override
  public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
    if (!StringUtils.hasLength(s)) return false;

    try {
      return HttpMethod.valueOf(s) != null;
    } catch (Exception ex) {
      return false;
    }
  }
}
