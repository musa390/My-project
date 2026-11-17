package com.luxeperfume.backend.checkout.pricing;

import com.luxeperfume.backend.checkout.domain.Coupon;
import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class AbstractCouponDiscountStrategy implements CouponDiscountStrategy {
    @Override
    public final BigDecimal calculate(Coupon coupon) {
        if (coupon == null || !supports(coupon.discountType())) throw new IllegalArgumentException("Coupon strategy does not support this coupon");
        BigDecimal raw = calculateRaw(coupon).setScale(2, RoundingMode.HALF_UP);
        if (raw.signum() < 0) throw new IllegalStateException("A discount strategy cannot return a negative discount");
        if (coupon.maximumDiscountAmount() != null) raw = raw.min(coupon.maximumDiscountAmount());
        return raw.min(coupon.eligibleSubtotal()).setScale(2, RoundingMode.HALF_UP);
    }

    protected abstract BigDecimal calculateRaw(Coupon coupon);
}
