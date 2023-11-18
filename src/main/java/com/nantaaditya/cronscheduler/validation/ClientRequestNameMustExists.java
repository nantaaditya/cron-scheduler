package com.nantaaditya.cronscheduler.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ClientRequestNameMustExistsValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClientRequestNameMustExists {
  String message() default "MustExists";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}