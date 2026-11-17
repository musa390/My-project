package com.luxeperfume.backend.checkout.policy;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public final class PreDispatchOrderCancellationPolicy implements OrderCancellationPolicy {
    private static final Set<String> PRE_DISPATCH = Set.of("PENDING", "CONFIRMED", "PROCESSING");
    private static final Set<String> REVERSIBLE_PAYMENT_STATES = Set.of("UNPAID", "FAILED");

    @Override
    public boolean mayCustomerCancel(String orderStatus, String paymentStatus) {
        return PRE_DISPATCH.contains(orderStatus) && REVERSIBLE_PAYMENT_STATES.contains(paymentStatus);
    }
}
