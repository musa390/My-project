package com.luxeperfume.backend.checkout.pricing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public final class PercentageTaxPolicy implements TaxPolicy {
    private final BigDecimal percentage;

    public PercentageTaxPolicy(@Value("${app.checkout.tax.percentage:0.00}") BigDecimal percentage) {
        if (percentage == null || percentage.signum() < 0 || percentage.compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("Checkout tax percentage must be between 0 and 100");
        this.percentage = percentage;
    }

    @Override
    public BigDecimal calculate(BigDecimal taxableAmount) {
        return taxableAmount.multiply(percentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
