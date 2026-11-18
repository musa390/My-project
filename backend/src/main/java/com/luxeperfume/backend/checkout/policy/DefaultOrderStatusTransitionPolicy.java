package com.luxeperfume.backend.checkout.policy;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public final class DefaultOrderStatusTransitionPolicy implements OrderStatusTransitionPolicy {
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "PENDING", Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", Set.of("PROCESSING", "CANCELLED"),
            "PROCESSING", Set.of("SHIPPED", "CANCELLED"),
            "SHIPPED", Set.of("DELIVERED"),
            "DELIVERED", Set.of("RETURNED")
    );

    @Override
    public boolean mayTransition(String currentStatus, String requestedStatus) {
        return ALLOWED.getOrDefault(currentStatus, Set.of()).contains(requestedStatus);
    }
}
