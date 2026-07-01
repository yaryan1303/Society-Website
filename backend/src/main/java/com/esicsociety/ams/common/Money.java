package com.esicsociety.ams.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Helpers for money math. All monetary values are {@link BigDecimal} with a
 * scale of 2 (rupees + paise). Never use float/double for currency.
 */
public final class Money {

    public static final BigDecimal ZERO = scale(BigDecimal.ZERO);

    private Money() {}

    /** Normalise to 2 decimal places, HALF_UP. Null is treated as zero. */
    public static BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal nz(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return scale(nz(a).add(nz(b)));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return scale(nz(a).subtract(nz(b)));
    }

    public static boolean isPositive(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isNegative(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) < 0;
    }

    public static boolean gt(BigDecimal a, BigDecimal b) {
        return nz(a).compareTo(nz(b)) > 0;
    }
}
