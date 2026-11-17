package com.luxeperfume.backend.checkout.inventory;

import com.luxeperfume.backend.checkout.domain.CheckoutLine;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class JdbcInventoryReservationService implements InventoryReservationService {
    private final JdbcTemplate jdbc;

    public JdbcInventoryReservationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void reserve(long orderId, String orderNumber, List<CheckoutLine> lines) {
        // The caller locks variant and inventory rows in ascending variant ID order.
        // Positive RESERVATION movement means quantity_reserved increases by this amount.
        for (CheckoutLine line : lines) {
            int changed = jdbc.update("""
                    UPDATE inventory
                    SET quantity_reserved = quantity_reserved + ?
                    WHERE variant_id = ?
                      AND quantity_on_hand >= quantity_reserved + ?
                    """, line.quantity(), line.variantId(), line.quantity());
            if (changed != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stock changed while placing the order. Please review your cart.");
            }
            jdbc.update("""
                    INSERT INTO inventory_movements
                        (variant_id,movement_type,quantity_change,reference_type,reference_id,note)
                    VALUES(?,'RESERVATION',?,'ORDER',?,?)
                    """, line.variantId(), line.quantity(), orderId,
                    "Reserved for order " + orderNumber);
        }
    }
}
