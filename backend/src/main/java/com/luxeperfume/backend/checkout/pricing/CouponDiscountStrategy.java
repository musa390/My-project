package com.luxeperfume.backend.checkout.pricing;

import com.luxeperfume.backend.checkout.domain.Coupon;
import java.math.BigDecimal;

public interface CouponDiscountStrategy {
    boolean supports(String discountType);
    BigDecimal calculate(Coupon coupon);
}
