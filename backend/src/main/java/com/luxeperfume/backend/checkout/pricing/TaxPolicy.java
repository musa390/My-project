package com.luxeperfume.backend.checkout.pricing;

import java.math.BigDecimal;

public interface TaxPolicy {
    BigDecimal calculate(BigDecimal taxableAmount);
}
