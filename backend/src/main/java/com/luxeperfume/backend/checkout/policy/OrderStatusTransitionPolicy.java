package com.luxeperfume.backend.checkout.policy;

/** Encapsulates the allowed operational order lifecycle transitions. */
public interface OrderStatusTransitionPolicy {
    boolean mayTransition(String currentStatus, String requestedStatus);
}
