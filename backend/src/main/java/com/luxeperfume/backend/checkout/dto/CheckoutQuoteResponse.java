package com.luxeperfume.backend.checkout.dto;

import com.luxeperfume.backend.checkout.pricing.CheckoutQuoteService;
import com.luxeperfume.backend.checkout.domain.MoneyBreakdown;
import java.math.BigDecimal;
import java.util.List;

public record CheckoutQuoteResponse(
        long addressId,
        String recipientName,
        String phone,
        String addressLine1,
        String addressLine2,
        String area,
        String city,
        String district,
        String postalCode,
        String countryCode,
        String customerEmail,
        String currency,
        String couponCode,
        Money totals,
        List<CheckoutQuoteService.QuotedLine> items,
        boolean stockAvailable,
        String notice
) {
    public CheckoutQuoteResponse { items = List.copyOf(items); }

    public record Money(BigDecimal subtotal, BigDecimal discountTotal, BigDecimal shippingTotal,
                        BigDecimal taxTotal, BigDecimal grandTotal) {
        public static Money from(MoneyBreakdown value) {
            return new Money(value.subtotal(), value.discountTotal(), value.shippingTotal(),
                    value.taxTotal(), value.grandTotal());
        }
    }
}
