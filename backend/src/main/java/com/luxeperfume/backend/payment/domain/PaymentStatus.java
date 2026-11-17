package com.luxeperfume.backend.payment.domain;

public enum PaymentStatus {
    INITIATED,
    PENDING,
    AUTHORIZED,
    PAID,
    FAILED,
    CANCELLED,
    REFUNDED,
    PARTIALLY_REFUNDED
}
