package com.luxeperfume.backend.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** [ENC] Immutable money aggregate; totals are derived and cannot be externally mutated. */
public final class MoneyBreakdown {
    private final BigDecimal subtotal;
    private final BigDecimal discountTotal;
    private final BigDecimal shippingTotal;
    private final BigDecimal taxTotal;
    private final BigDecimal grandTotal;

    public MoneyBreakdown(BigDecimal subtotal, BigDecimal discountTotal,
                          BigDecimal shippingTotal, BigDecimal taxTotal) {
        this.subtotal = money(subtotal);
        this.discountTotal = money(discountTotal);
        this.shippingTotal = money(shippingTotal);
        this.taxTotal = money(taxTotal);
        if (this.discountTotal.compareTo(this.subtotal) > 0)
            throw new IllegalArgumentException("Discount cannot exceed subtotal");
        this.grandTotal = this.subtotal.subtract(this.discountTotal)
                .add(this.shippingTotal).add(this.taxTotal).setScale(2, RoundingMode.HALF_UP);
        if (this.grandTotal.signum() < 0) throw new IllegalArgumentException("Grand total cannot be negative");
    }

    public BigDecimal subtotal() { return subtotal; }
    public BigDecimal discountTotal() { return discountTotal; }
    public BigDecimal shippingTotal() { return shippingTotal; }
    public BigDecimal taxTotal() { return taxTotal; }
    public BigDecimal grandTotal() { return grandTotal; }

    private static BigDecimal money(BigDecimal value) {
        Objects.requireNonNull(value, "Money amount is required");
        if (value.signum() < 0) throw new IllegalArgumentException("Money amount cannot be negative");
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
