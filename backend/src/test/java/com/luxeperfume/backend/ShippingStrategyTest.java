package com.luxeperfume.backend;

import com.luxeperfume.backend.checkout.domain.CheckoutLine;
import com.luxeperfume.backend.checkout.pricing.CheckoutQuoteService;
import com.luxeperfume.backend.checkout.pricing.FlatRateShippingPolicy;
import com.luxeperfume.backend.checkout.pricing.FreeShippingThresholdPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShippingStrategyTest {
    @Test
    void polymorphicShippingSelectionUsesThresholdThenFallsBackToFlatRate() {
        var strategies = List.of(
                new FreeShippingThresholdPolicy(new BigDecimal("1000.00")),
                new FlatRateShippingPolicy(new BigDecimal("80.00")));
        var quoteService = new CheckoutQuoteService(List.of(), strategies, taxable -> BigDecimal.ZERO);

        CheckoutLine expensive = line("1200.00");
        CheckoutLine affordable = line("500.00");

        var freeQuote = quoteService.calculate(List.of(expensive), null, "BD", "Dhaka", "Gulshan");
        var paidQuote = quoteService.calculate(List.of(affordable), null, "BD", "Dhaka", "Gulshan");

        assertThat(freeQuote.totals().shippingTotal()).isEqualByComparingTo("0.00");
        assertThat(paidQuote.totals().shippingTotal()).isEqualByComparingTo("80.00");
    }

    private static CheckoutLine line(String price) {
        return new CheckoutLine(1, 1, 1, "Test Perfume", "50ml", "TEST-50",
                1, new BigDecimal(price), 10);
    }
}
