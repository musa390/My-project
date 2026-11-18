package com.luxeperfume.backend.payment.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {
    @Test
    void encapsulatesAndTransitionsPaymentState() {
        Payment payment = new Payment(1, 2, PaymentProvider.SSLCOMMERZ,
                new BigDecimal("1250.00"), "BDT", PaymentStatus.PENDING);
        payment.transitionTo(PaymentStatus.PAID);
        assertEquals(PaymentStatus.PAID, payment.status());
        assertEquals(new BigDecimal("1250.00"), payment.amount());
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> new Payment(1, 2,
                PaymentProvider.BKASH, BigDecimal.ZERO, "BDT", PaymentStatus.INITIATED));
    }

    @Test
    void rejectsInvalidCurrency() {
        assertThrows(IllegalArgumentException.class, () -> new Payment(1, 2,
                PaymentProvider.NAGAD, new BigDecimal("100.00"), "BD", PaymentStatus.INITIATED));
    }
}
