package com.nantaaditya.cronscheduler.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpMethodMustValidValidatorTest {

  @InjectMocks
  private HttpMethodMustValidValidator validator;

  @Test
  void isValid() {
    assertFalse(validator.isValid("", null));
    assertFalse(validator.isValid("OTHER", null));
  }
}