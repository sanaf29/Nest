package com.nest.validation;

import com.nest.model.Currencies;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Delegates to the domain's {@link Currencies} so API validation and entity invariants agree. */
public class CurrencyCodeValidator implements ConstraintValidator<ValidCurrency, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || Currencies.isSupported(value);
    }
}
