package com.luxeperfume.backend.checkout.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderPlacementResponse(
        long orderId,
        String orderNumber,
        String orderStatus,
        String paymentStatus,
        String currency,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal shippingTotal,
        BigDecimal taxTotal,
        BigDecimal grandTotal,
        String couponCode,
        String shippingRecipient,
        String shippingPhone,
        String shippingAddress,
        Instant placedAt,
        List<Item> items
) {
    public OrderPlacementResponse {
        items = List.copyOf(items);
    }

    public record Item(
            long productId,
            long variantId,
            String productName,
            String variantName,
            String sku,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineDiscount,
            BigDecimal lineTotal
    ) {}
}
