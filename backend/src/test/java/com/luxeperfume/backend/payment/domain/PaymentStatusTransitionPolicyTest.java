package com.luxeperfume.backend.payment.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentStatusTransitionPolicyTest {
    @Test
    void pendingCanBecomePaidAfterServerVerification() {
        assertTrue(PaymentStatusTransitionPolicy.canTransition(PaymentStatus.PENDING, PaymentStatus.PAID));
    }

    @Test
    void paidCannotBeDowngradedByLateFailureCallback() {
        assertFalse(PaymentStatusTransitionPolicy.canTransition(PaymentStatus.PAID, PaymentStatus.FAILED));
    }

    @Test
    void failedIsTerminal() {
        assertFalse(PaymentStatusTransitionPolicy.canTransition(PaymentStatus.FAILED, PaymentStatus.PAID));
    }

    @Test
    void paidCanOnlyMoveIntoRefundStates() {
        assertTrue(PaymentStatusTransitionPolicy.canTransition(PaymentStatus.PAID, PaymentStatus.PARTIALLY_REFUNDED));
        assertTrue(PaymentStatusTransitionPolicy.canTransition(PaymentStatus.PAID, PaymentStatus.REFUNDED));
    }

    @Test
    void invalidTransitionThrows() {
        assertThrows(IllegalStateException.class,
                () -> PaymentStatusTransitionPolicy.requireTransition(PaymentStatus.CANCELLED, PaymentStatus.PAID));
    }
}
