package com.luxeperfume.backend.payment.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Small domain aggregate; persistence and provider payloads stay outside this object. */
/** [ENC] Payment aggregate owns its state and permits only policy-approved transitions. */
public final class Payment {
    private final long id;
    private final long orderId;
    private final PaymentProvider provider;
    private final BigDecimal amount;
    private final String currency;
    private PaymentStatus status;

    public Payment(long id, long orderId, PaymentProvider provider,
                   BigDecimal amount, String currency, PaymentStatus status) {
        if (id < 0 || orderId <= 0) throw new IllegalArgumentException("Invalid payment/order id");
        this.id = id;
        this.orderId = orderId;
        this.provider = Objects.requireNonNull(provider, "provider");
        this.amount = Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0 || amount.scale() > 2) throw new IllegalArgumentException("Invalid payment amount");
        if (currency == null || !currency.matches("[A-Z]{3}")) throw new IllegalArgumentException("Invalid currency");
        this.currency = currency;
        this.status = Objects.requireNonNull(status, "status");
    }

    public long id() { return id; }
    public long orderId() { return orderId; }
    public PaymentProvider provider() { return provider; }
    public BigDecimal amount() { return amount; }
    public String currency() { return currency; }
    public PaymentStatus status() { return status; }

    public void transitionTo(PaymentStatus next) {
        this.status = PaymentStatusTransitionPolicy.requireTransition(this.status, next);
    }
}
