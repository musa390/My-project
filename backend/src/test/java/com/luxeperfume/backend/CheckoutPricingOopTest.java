package com.luxeperfume.backend;

import com.luxeperfume.backend.checkout.domain.Coupon;
import com.luxeperfume.backend.checkout.domain.MoneyBreakdown;
import com.luxeperfume.backend.checkout.pricing.FixedCouponDiscountStrategy;
import com.luxeperfume.backend.checkout.pricing.PercentageCouponDiscountStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutPricingOopTest {
    private final PercentageCouponDiscountStrategy percentage = new PercentageCouponDiscountStrategy();
    private final FixedCouponDiscountStrategy fixed = new FixedCouponDiscountStrategy();

    @Test
    void polymorphismSelectsPercentageAndFixedStrategiesThroughTheSameContract() {
        var strategies = java.util.List.of(percentage, fixed);
        Coupon percent = coupon("PERCENTAGE", "10.00", "500.00", "1000.00");
        Coupon fixedAmount = coupon("FIXED", "250.00", null, "1000.00");

        var percentStrategy = strategies.stream().filter(s -> s.supports(percent.discountType())).findFirst().orElseThrow();
        var fixedStrategy = strategies.stream().filter(s -> s.supports(fixedAmount.discountType())).findFirst().orElseThrow();

        assertThat(percentStrategy.calculate(percent)).isEqualByComparingTo("100.00");
        assertThat(fixedStrategy.calculate(fixedAmount)).isEqualByComparingTo("250.00");
    }

    @Test
    void percentageStrategyAppliesMaximumDiscountAndNeverExceedsEligibleSubtotal() {
        Coupon capped = coupon("PERCENTAGE", "50.00", "120.00", "1000.00");
        Coupon smallerCart = coupon("FIXED", "500.00", null, "80.00");

        assertThat(percentage.calculate(capped)).isEqualByComparingTo("120.00");
        assertThat(fixed.calculate(smallerCart)).isEqualByComparingTo("80.00");
    }

    @Test
    void moneyBreakdownEncapsulatesNonNegativeAndDiscountInvariants() {
        MoneyBreakdown totals = new MoneyBreakdown(
                new BigDecimal("1000"), new BigDecimal("100"), new BigDecimal("60"), new BigDecimal("0"));
        assertThat(totals.grandTotal()).isEqualByComparingTo("960.00");
        assertThatThrownBy(() -> new MoneyBreakdown(
                new BigDecimal("100"), new BigDecimal("101"), BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MoneyBreakdown(
                new BigDecimal("-1"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Coupon coupon(String type, String value, String cap, String eligible) {
        return new Coupon("TEST", type, new BigDecimal(value), BigDecimal.ZERO,
                cap == null ? null : new BigDecimal(cap), new BigDecimal(eligible), Set.of(), Set.of(), false);
    }
}
