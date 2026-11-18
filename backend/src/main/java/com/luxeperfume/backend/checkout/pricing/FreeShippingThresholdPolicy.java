package com.luxeperfume.backend.checkout.pricing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** [POL] Free-shipping strategy selected when the configured order subtotal threshold is reached. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class FreeShippingThresholdPolicy implements ShippingPolicy {
    private final BigDecimal threshold;

    public FreeShippingThresholdPolicy(
            @Value("${app.checkout.shipping.free-threshold:0.00}") BigDecimal threshold) {
        if (threshold == null || threshold.signum() < 0) {
            throw new IllegalArgumentException("Free-shipping threshold must be non-negative");
        }
        this.threshold = threshold.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public boolean supports(BigDecimal subtotal, String countryCode, String city, String area) {
        return threshold.signum() > 0 && subtotal != null && subtotal.compareTo(threshold) >= 0;
    }

    @Override
    public BigDecimal calculate(String countryCode, String city, String area) {
        return BigDecimal.ZERO.setScale(2);
    }
}
