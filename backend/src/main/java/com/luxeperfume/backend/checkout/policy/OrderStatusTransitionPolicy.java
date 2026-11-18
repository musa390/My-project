package com.luxeperfume.backend.checkout.policy;

/** Encapsulates the allowed operational order lifecycle transitions. */
/** [ABS] Order lifecycle transition contract. */
public interface OrderStatusTransitionPolicy {
    boolean mayTransition(String currentStatus, String requestedStatus);
}
