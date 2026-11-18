package com.luxeperfume.backend.checkout.pricing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public final class FlatRateShippingPolicy implements ShippingPolicy {
    private final BigDecimal flatRate;

    public FlatRateShippingPolicy(@Value("${app.checkout.shipping.flat-rate:0.00}") BigDecimal flatRate) {
        if (flatRate == null || flatRate.signum() < 0) throw new IllegalArgumentException("Checkout shipping flat rate must be non-negative");
        this.flatRate = flatRate.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean supports(BigDecimal subtotal, String countryCode, String city, String area) {
        return true;
    }

    @Override public BigDecimal calculate(String countryCode, String city, String area) { return flatRate; }
}
