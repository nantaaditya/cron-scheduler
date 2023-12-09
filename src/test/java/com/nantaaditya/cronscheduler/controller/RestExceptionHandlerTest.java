package com.nantaaditya.cronscheduler.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ExtendWith(MockitoExtension.class)
class RestExceptionHandlerTest {

  @Mock
  private MethodArgumentNotValidException methodArgumentNotValidException;

  @Mock
  private ConstraintViolationException constraintViolationException;

  @Mock
  private BindingResult bindingResult;

  @Mock
  private ConstraintViolation constraintViolation;

  @Mock
  private FieldError fieldError;

  @Mock
  private Path path;

  @InjectMocks
  private RestExceptionHandler restExceptionHandler;

  @Test
  void handleValidationExceptions() {
    when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));
    when(fieldError.getField()).thenReturn("key");
    when(fieldError.getDefaultMessage()).thenReturn("error");

    assertNotNull(restExceptionHandler.handleValidationExceptions(methodArgumentNotValidException));

    verify(methodArgumentNotValidException).getBindingResult();
    verify(bindingResult).getAllErrors();
    verify(fieldError).getField();
    verify(fieldError).getDefaultMessage();
  }

  @Test
  void constraintViolationException() {
    when(constraintViolationException.getConstraintViolations()).thenReturn(Set.of(constraintViolation));
    when(constraintViolation.getPropertyPath()).thenReturn(path);
    when(path.toString()).thenReturn("key.subKey");
    when(constraintViolation.getMessage()).thenReturn("message");

    assertNotNull(restExceptionHandler.constraintViolationException(constraintViolationException));

    verify(constraintViolationException).getConstraintViolations();
    verify(constraintViolation).getPropertyPath();
    verify(constraintViolation).getMessage();
  }
}