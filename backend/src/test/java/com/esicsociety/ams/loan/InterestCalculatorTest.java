package com.esicsociety.ams.loan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for the interest engine — no Spring, no database.
 * These figures are taken straight off the reference ledger (A/c 1583) and the
 * code MUST reproduce them exactly.
 */
class InterestCalculatorTest {

    private static final BigDecimal RATE_8 = new BigDecimal("8");

    @ParameterizedTest(name = "{0} @ 8% p.a. -> {1}/month")
    @CsvSource({
            "183000, 1220",
            "173000, 1153",
            "163000, 1087",
            "153000, 1020",
            "146000,  973",
            "139000,  927",
            "132000,  880"
    })
    @DisplayName("reproduces the sample ledger interest figures exactly")
    void reproducesLedgerFigures(String outstanding, String expected) {
        BigDecimal result = InterestCalculator.monthlyInterest(new BigDecimal(outstanding), RATE_8);
        assertThat(result).isEqualByComparingTo(new BigDecimal(expected));
    }

    @Test
    @DisplayName("result is rounded to the nearest rupee (HALF_UP) with scale 2")
    void roundsToNearestRupee() {
        // 173000 * 0.08 / 12 = 1153.333... -> 1153
        assertThat(InterestCalculator.monthlyInterest(new BigDecimal("173000"), RATE_8))
                .isEqualByComparingTo("1153");
        // 163000 * 0.08 / 12 = 1086.666... -> 1087
        assertThat(InterestCalculator.monthlyInterest(new BigDecimal("163000"), RATE_8))
                .isEqualByComparingTo("1087");
        assertThat(InterestCalculator.monthlyInterest(new BigDecimal("183000"), RATE_8).scale())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("zero / negative / null balances yield zero")
    void nonPositiveBalances() {
        assertThat(InterestCalculator.monthlyInterest(BigDecimal.ZERO, RATE_8)).isEqualByComparingTo("0");
        assertThat(InterestCalculator.monthlyInterest(new BigDecimal("-100"), RATE_8)).isEqualByComparingTo("0");
        assertThat(InterestCalculator.monthlyInterest(null, RATE_8)).isEqualByComparingTo("0");
        assertThat(InterestCalculator.monthlyInterest(new BigDecimal("100000"), null)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("a different rate is applied correctly")
    void honoursRate() {
        // 120000 * 0.10 / 12 = 1000
        assertThat(InterestCalculator.monthlyInterest(new BigDecimal("120000"), new BigDecimal("10")))
                .isEqualByComparingTo("1000");
    }
}
