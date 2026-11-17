package com.luxeperfume.backend.checkout.pricing;

import java.math.BigDecimal;

public interface ShippingPolicy {
    BigDecimal calculate(String countryCode, String city, String area);
}
