package com.luxeperfume.backend.payment.domain;

/** Centralized monotonic payment-state rules. A browser redirect is never evidence of PAID. */
public final class PaymentStatusTransitionPolicy {
    private PaymentStatusTransitionPolicy() {}

    public static boolean canTransition(PaymentStatus current, PaymentStatus next) {
        if (current == null || next == null || current == next) return false;
        return switch (current) {
            case INITIATED -> next == PaymentStatus.PENDING || next == PaymentStatus.FAILED
                    || next == PaymentStatus.CANCELLED;
            case PENDING -> next == PaymentStatus.AUTHORIZED || next == PaymentStatus.PAID
                    || next == PaymentStatus.FAILED || next == PaymentStatus.CANCELLED;
            case AUTHORIZED -> next == PaymentStatus.PAID || next == PaymentStatus.FAILED
                    || next == PaymentStatus.CANCELLED;
            case PAID -> next == PaymentStatus.PARTIALLY_REFUNDED || next == PaymentStatus.REFUNDED;
            case PARTIALLY_REFUNDED -> next == PaymentStatus.REFUNDED;
            case FAILED, CANCELLED, REFUNDED -> false;
        };
    }

    public static PaymentStatus requireTransition(PaymentStatus current, PaymentStatus next) {
        if (!canTransition(current, next)) {
            throw new IllegalStateException("Invalid payment status transition: " + current + " -> " + next);
        }
        return next;
    }
}
