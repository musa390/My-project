package com.luxeperfume.backend.checkout.pricing;

import com.luxeperfume.backend.checkout.domain.Coupon;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public final class FixedCouponDiscountStrategy extends AbstractCouponDiscountStrategy {
    @Override public boolean supports(String discountType) { return "FIXED".equals(discountType); }

    @Override
    protected BigDecimal calculateRaw(Coupon coupon) {
        return coupon.discountValue();
    }
}
