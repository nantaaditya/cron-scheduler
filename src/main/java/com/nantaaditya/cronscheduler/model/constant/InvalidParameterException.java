package com.nantaaditya.cronscheduler.model.constant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InvalidParameterException extends RuntimeException {

  private Map<String, List<String>> violations = new HashMap<>();

  public InvalidParameterException(Map<String, List<String>> violations, String message) {
    super(message);
    this.violations = violations;
  }
}
