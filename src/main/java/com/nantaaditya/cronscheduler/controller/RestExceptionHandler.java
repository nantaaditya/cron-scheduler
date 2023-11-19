package com.nantaaditya.cronscheduler.controller;

import com.nantaaditya.cronscheduler.model.constant.InvalidParameterException;
import com.nantaaditya.cronscheduler.model.response.Response;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Response handleValidationExceptions(MethodArgumentNotValidException ex) {
    log.error("#ERROR - handle validation exception, ", ex);
    Map<String, List<String>> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(error -> {
          String fieldName = ((FieldError) error).getField();
          String errorMessage = error.getDefaultMessage();
          errors.put(fieldName, List.of(errorMessage));
        });
    return Response.error(errors);
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(ConstraintViolationException.class)
  public Response constraintViolationException(ConstraintViolationException ex) {
    log.error("#ERROR - constraint violation exception, ", ex);
    Map<String, List<String>> errors = new HashMap<>();
    ex.getConstraintViolations()
        .forEach(violation ->
          putEntry(errors, getField(violation.getPropertyPath()), violation.getMessage())
        );
    return Response.error(errors);
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(InvalidParameterException.class)
  public Response invalidParameterException(InvalidParameterException ex) {
    log.error("#ERROR - invalid parameter exception, ", ex);
    return Response.error(ex.getViolations());
  }

  private void putEntry(Map<String, List<String>> map, String key, String value) {
    map.computeIfAbsent(key, r -> new LinkedList<>());
    map.get(key).add(value);
  }

  private String getField(Path path) {
    String[] pathString = path.toString().split("\\.");
    return pathString[pathString.length-1];
  }
}
