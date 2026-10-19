package com.nest.model;

import com.nest.exception.BusinessRuleException;

import java.util.Currency;
import java.util.regex.Pattern;

/**
 * Domain knowledge about ISO 4217 currency codes.
 *
 * <p>A currency is supported when it is a real ISO 4217 code with defined minor units.
 * Pseudo-currencies such as XAU (gold) or XXX (no currency) have no minor units and are rejected.
 */
public final class Currencies {

    private static final Pattern ISO_CODE = Pattern.compile("[A-Z]{3}");

    private Currencies() {
    }

    public static boolean isSupported(String code) {
        if (code == null || !ISO_CODE.matcher(code).matches()) {
            return false;
        }
        try {
            return Currency.getInstance(code).getDefaultFractionDigits() >= 0;
        } catch (IllegalArgumentException unknownCode) {
            return false;
        }
    }

    /** Returns the code unchanged if supported, otherwise throws a {@link BusinessRuleException}. */
    public static String requireSupported(String code) {
        if (!isSupported(code)) {
            throw new BusinessRuleException("'%s' is not a supported ISO 4217 currency code.".formatted(code));
        }
        return code;
    }

    /** Number of decimal places the currency allows (2 for CAD, 0 for JPY, 3 for BHD). */
    public static int minorUnits(String code) {
        return Currency.getInstance(requireSupported(code)).getDefaultFractionDigits();
    }
}
