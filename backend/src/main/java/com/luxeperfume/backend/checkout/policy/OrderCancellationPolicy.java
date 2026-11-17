package com.luxeperfume.backend.checkout.policy;

/**
 * Abstraction for deciding whether a customer may cancel an order.
 * Keeping the rule behind an interface allows future policy variants without
 * putting business rules in the HTTP controller or persistence code.
 */
public interface OrderCancellationPolicy {
    boolean mayCustomerCancel(String orderStatus, String paymentStatus);
}
