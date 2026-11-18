package com.luxeperfume.backend.returns.domain;

/** [POL] Only delivered orders can enter the customer return-review workflow. */
public final class ReturnEligibilityPolicy {
    private ReturnEligibilityPolicy() {}

    public static void requireEligible(String orderStatus) {
        if (!"DELIVERED".equals(orderStatus)) {
            throw new IllegalArgumentException("A return can only be requested for a delivered order");
        }
    }
}
