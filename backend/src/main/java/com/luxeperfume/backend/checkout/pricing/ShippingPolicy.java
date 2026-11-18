package com.luxeperfume.backend.checkout.pricing;

import java.math.BigDecimal;

/** [ABS] Shipping strategy contract selected by the checkout pricing service. */
public interface ShippingPolicy {
    /** [POL] Allows the checkout to select a concrete shipping strategy for this quote. */
    boolean supports(BigDecimal subtotal, String countryCode, String city, String area);

    BigDecimal calculate(String countryCode, String city, String area);
}
