package com.luxeperfume.backend.checkout.inventory;

import com.luxeperfume.backend.checkout.domain.CheckoutLine;

import java.util.List;

public interface InventoryReservationService {
    void reserve(long orderId, String orderNumber, List<CheckoutLine> lines);
}
