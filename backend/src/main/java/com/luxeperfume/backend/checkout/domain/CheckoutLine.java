package com.luxeperfume.backend.checkout.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** [ENC] Immutable checkout snapshot with validated identifiers, quantity and unit price. */
public final class CheckoutLine {
    private final long productId;
    private final long categoryId;
    private final long variantId;
    private final String productName;
    private final String variantName;
    private final String sku;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final long availableStock;

    public CheckoutLine(long productId, long categoryId, long variantId, String productName,
                        String variantName, String sku, int quantity, BigDecimal unitPrice, long availableStock) {
        if (productId <= 0 || categoryId <= 0 || variantId <= 0) throw new IllegalArgumentException("Product, category and variant IDs must be positive");
        if (quantity <= 0 || quantity > 99) throw new IllegalArgumentException("Quantity must be between 1 and 99");
        if (availableStock < 0) throw new IllegalArgumentException("Available stock cannot be negative");
        this.productId = productId;
        this.categoryId = categoryId;
        this.variantId = variantId;
        this.productName = Objects.requireNonNull(productName);
        this.variantName = Objects.requireNonNull(variantName);
        this.sku = Objects.requireNonNull(sku);
        this.quantity = quantity;
        this.unitPrice = requireMoney(unitPrice, "Unit price");
        this.availableStock = availableStock;
    }

    public long productId() { return productId; }
    public long categoryId() { return categoryId; }
    public long variantId() { return variantId; }
    public String productName() { return productName; }
    public String variantName() { return variantName; }
    public String sku() { return sku; }
    public int quantity() { return quantity; }
    public BigDecimal unitPrice() { return unitPrice; }
    public long availableStock() { return availableStock; }
    public BigDecimal lineSubtotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2); }

    private static BigDecimal requireMoney(BigDecimal value, String label) {
        Objects.requireNonNull(value, label + " is required");
        if (value.signum() < 0) throw new IllegalArgumentException(label + " cannot be negative");
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
