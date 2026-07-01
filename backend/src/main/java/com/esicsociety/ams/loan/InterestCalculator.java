package com.esicsociety.ams.loan;

import com.esicsociety.ams.config.AppProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Simple-interest, reducing-balance engine.
 *
 * <p>Each month interest is charged only on the current outstanding loan balance:
 * <pre>monthly_interest = outstanding × (annualRatePct / 100) ÷ 12</pre>
 * rounded to the nearest rupee (HALF_UP), exactly as the paper ledger does.
 *
 * <p>Verified against Yogesh Kr. Yadav's loan page (A/c 1583) at 8% p.a.:
 * <pre>
 *   183000 → 1220   173000 → 1153   163000 → 1087   153000 → 1020
 *   146000 →  973   139000 →  927   132000 →  880
 * </pre>
 *
 * <p>The core method is {@code static} and side-effect free so it can be unit
 * tested without any Spring context or database.
 */
@Component
public class InterestCalculator {

    private static final BigDecimal HUNDRED_X_TWELVE = new BigDecimal("1200");

    private final BigDecimal defaultAnnualRatePct;

    public InterestCalculator(AppProperties props) {
        this.defaultAnnualRatePct = props.getLoanAnnualRatePct();
    }

    /** Monthly interest on {@code outstanding} at the configured default rate. */
    public BigDecimal monthlyInterest(BigDecimal outstanding) {
        return monthlyInterest(outstanding, defaultAnnualRatePct);
    }

    public BigDecimal annualRatePct() {
        return defaultAnnualRatePct;
    }

    /**
     * Monthly interest = outstanding × annualRatePct ÷ 1200, rounded to the
     * nearest rupee and returned with scale 2 (e.g. {@code 1220.00}).
     * Non-positive balances yield zero.
     */
    public static BigDecimal monthlyInterest(BigDecimal outstanding, BigDecimal annualRatePct) {
        if (outstanding == null || outstanding.signum() <= 0
                || annualRatePct == null || annualRatePct.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return outstanding
                .multiply(annualRatePct)
                .divide(HUNDRED_X_TWELVE, 0, RoundingMode.HALF_UP) // round to nearest rupee
                .setScale(2, RoundingMode.HALF_UP);
    }
}
