package com.nest.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated string must be a supported, upper-case ISO 4217 currency code (e.g. {@code CAD}).
 * {@code null} is valid; combine with {@code @NotNull} when the field is required.
 */
@Documented
@Constraint(validatedBy = CurrencyCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCurrency {

    String message() default "must be a supported ISO 4217 currency code, e.g. CAD";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
