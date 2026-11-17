package com.luxeperfume.backend.checkout.pricing;

import com.luxeperfume.backend.checkout.domain.Coupon;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public final class PercentageCouponDiscountStrategy extends AbstractCouponDiscountStrategy {
    @Override public boolean supports(String discountType) { return "PERCENTAGE".equals(discountType); }

    @Override
    protected BigDecimal calculateRaw(Coupon coupon) {
        if (coupon.discountValue().compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("Percentage discount cannot exceed 100");
        return coupon.eligibleSubtotal().multiply(coupon.discountValue())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
