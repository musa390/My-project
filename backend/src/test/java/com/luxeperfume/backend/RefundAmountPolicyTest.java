package com.luxeperfume.backend;

import com.luxeperfume.backend.payment.domain.RefundAmountPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundAmountPolicyTest {
    @Test
    void calculatesRemainingRefundableBalanceAndAllowsPartialRefunds() {
        BigDecimal remaining = RefundAmountPolicy.remaining(
                new BigDecimal("1000.00"), new BigDecimal("250.00"));
        assertThat(remaining).isEqualByComparingTo("750.00");
        RefundAmountPolicy.requireWithinRemaining(new BigDecimal("700.00"), remaining);
    }

    @Test
    void rejectsOverRefundAndCorruptReservedTotals() {
        BigDecimal remaining = RefundAmountPolicy.remaining(
                new BigDecimal("1000.00"), new BigDecimal("250.00"));
        assertThatThrownBy(() -> RefundAmountPolicy.requireWithinRemaining(
                new BigDecimal("750.01"), remaining))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds");
        assertThatThrownBy(() -> RefundAmountPolicy.remaining(
                new BigDecimal("100.00"), new BigDecimal("101.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidPrecisionAndNonPositiveAmounts() {
        assertThatThrownBy(() -> RefundAmountPolicy.requireWithinRemaining(
                new BigDecimal("1.001"), new BigDecimal("10.00")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RefundAmountPolicy.requireWithinRemaining(
                BigDecimal.ZERO, new BigDecimal("10.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
