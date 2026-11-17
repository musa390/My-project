package com.luxeperfume.backend.checkout.domain;

import java.math.BigDecimal;
import java.util.Set;

public record Coupon(String code, String discountType, BigDecimal discountValue,
                     BigDecimal minimumOrderAmount, BigDecimal maximumDiscountAmount,
                     BigDecimal eligibleSubtotal, Set<Long> eligibleProductIds,
                     Set<Long> eligibleCategoryIds, boolean restricted) {
    public Coupon {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Coupon code is required");
        if (!"PERCENTAGE".equals(discountType) && !"FIXED".equals(discountType))
            throw new IllegalArgumentException("Unsupported coupon type");
        if (discountValue == null || discountValue.signum() <= 0) throw new IllegalArgumentException("Coupon discount must be positive");
        if (eligibleSubtotal == null || eligibleSubtotal.signum() < 0) throw new IllegalArgumentException("Eligible subtotal is invalid");
        eligibleProductIds = Set.copyOf(eligibleProductIds);
        eligibleCategoryIds = Set.copyOf(eligibleCategoryIds);
    }

    public boolean appliesTo(CheckoutLine line) {
        if (!restricted) return true;
        return eligibleProductIds.contains(line.productId()) || eligibleCategoryIds.contains(line.categoryId());
    }
}
