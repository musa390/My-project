package com.luxeperfume.backend.checkout.pricing;

import com.luxeperfume.backend.checkout.domain.CheckoutLine;
import com.luxeperfume.backend.checkout.domain.Coupon;
import com.luxeperfume.backend.checkout.domain.MoneyBreakdown;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
/** [POL] Selects coupon and shipping implementations through their strategy contracts. */
public class CheckoutQuoteService {
    private final List<CouponDiscountStrategy> discountStrategies;
    private final List<ShippingPolicy> shippingPolicies;
    private final TaxPolicy taxPolicy;

    public CheckoutQuoteService(List<CouponDiscountStrategy> discountStrategies,
                                List<ShippingPolicy> shippingPolicies, TaxPolicy taxPolicy) {
        this.discountStrategies = List.copyOf(discountStrategies);
        this.shippingPolicies = List.copyOf(shippingPolicies);
        this.taxPolicy = taxPolicy;
    }

    public Quote calculate(List<CheckoutLine> lines, Coupon coupon,
                           String countryCode, String city, String area) {
        BigDecimal subtotal = lines.stream().map(CheckoutLine::lineSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal eligibleSubtotal = coupon == null ? BigDecimal.ZERO : lines.stream()
                .filter(coupon::appliesTo).map(CheckoutLine::lineSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = BigDecimal.ZERO.setScale(2);
        if (coupon != null) {
            if (eligibleSubtotal.signum() <= 0) throw new IllegalArgumentException("Coupon does not apply to items in this cart");
            Coupon effective = new Coupon(coupon.code(), coupon.discountType(), coupon.discountValue(),
                    coupon.minimumOrderAmount(), coupon.maximumDiscountAmount(), eligibleSubtotal,
                    coupon.eligibleProductIds(), coupon.eligibleCategoryIds(), coupon.restricted());
            CouponDiscountStrategy strategy = discountStrategies.stream()
                    .filter(candidate -> candidate.supports(effective.discountType())).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No coupon strategy registered for " + effective.discountType()));
            discount = strategy.calculate(effective);
        }
        ShippingPolicy shippingStrategy = shippingPolicies.stream()
                .filter(candidate -> candidate.supports(subtotal, countryCode, city, area))
                .findFirst().orElseThrow(() -> new IllegalStateException("No shipping strategy supports this destination"));
        BigDecimal shipping = shippingStrategy.calculate(countryCode, city, area)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxable = subtotal.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal tax = taxPolicy.calculate(taxable).setScale(2, RoundingMode.HALF_UP);
        MoneyBreakdown totals = new MoneyBreakdown(subtotal, discount, shipping, tax);
        List<QuotedLine> quotedLines = allocateDiscount(lines, coupon, discount, eligibleSubtotal);
        return new Quote(totals, quotedLines, coupon == null ? null : coupon.code());
    }

    private List<QuotedLine> allocateDiscount(List<CheckoutLine> lines, Coupon coupon,
                                               BigDecimal discount, BigDecimal eligibleSubtotal) {
        List<QuotedLine> result = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO.setScale(2);
        int lastEligible = -1;
        for (int i = 0; i < lines.size(); i++) if (coupon != null && coupon.appliesTo(lines.get(i))) lastEligible = i;
        for (int i = 0; i < lines.size(); i++) {
            CheckoutLine line = lines.get(i);
            BigDecimal base = line.lineSubtotal();
            BigDecimal lineDiscount = BigDecimal.ZERO.setScale(2);
            if (coupon != null && coupon.appliesTo(line) && discount.signum() > 0) {
                if (i == lastEligible) lineDiscount = discount.subtract(allocated);
                else {
                    lineDiscount = discount.multiply(base).divide(eligibleSubtotal, 2, RoundingMode.DOWN);
                    lineDiscount = lineDiscount.min(base);
                    allocated = allocated.add(lineDiscount);
                }
            }
            result.add(new QuotedLine(line.productId(), line.variantId(), line.productName(),
                    line.variantName(), line.sku(), line.quantity(), line.unitPrice(), line.availableStock(),
                    lineDiscount, base.subtract(lineDiscount).setScale(2, RoundingMode.HALF_UP)));
        }
        return result;
    }

    public record QuotedLine(long productId, long variantId, String productName, String variantName,
                             String sku, int quantity, BigDecimal unitPrice, long availableStock,
                             BigDecimal lineDiscount, BigDecimal lineTotal) {}
    public record Quote(MoneyBreakdown totals, List<QuotedLine> lines, String couponCode) {
        public Quote { lines = List.copyOf(lines); }
    }
}
