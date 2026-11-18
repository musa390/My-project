package com.luxeperfume.backend.payment.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** [POL] Central refund amount rule shared by every refund entry point. */
public final class RefundAmountPolicy {
    private RefundAmountPolicy() {}

    public static BigDecimal remaining(BigDecimal paymentAmount, BigDecimal reservedRefundAmount) {
        Objects.requireNonNull(paymentAmount, "Payment amount is required");
        Objects.requireNonNull(reservedRefundAmount, "Reserved refund amount is required");
        if (paymentAmount.signum() <= 0 || reservedRefundAmount.signum() < 0) {
            throw new IllegalArgumentException("Payment and reserved refund amounts are invalid");
        }
        BigDecimal total = paymentAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal reserved = reservedRefundAmount.setScale(2, RoundingMode.HALF_UP);
        if (reserved.compareTo(total) > 0) {
            throw new IllegalArgumentException("Reserved refunds cannot exceed the original payment amount");
        }
        return total.subtract(reserved).setScale(2, RoundingMode.HALF_UP);
    }

    public static void requireWithinRemaining(BigDecimal requestedAmount, BigDecimal remainingAmount) {
        Objects.requireNonNull(requestedAmount, "Requested refund amount is required");
        Objects.requireNonNull(remainingAmount, "Remaining refund amount is required");
        if (requestedAmount.signum() <= 0 || requestedAmount.scale() > 2) {
            throw new IllegalArgumentException("Refund amount must be positive with at most two decimal places");
        }
        if (requestedAmount.compareTo(remainingAmount) > 0) {
            throw new IllegalArgumentException("Refund amount exceeds the remaining refundable amount");
        }
    }
}
