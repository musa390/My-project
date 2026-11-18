package com.luxeperfume.backend;

import com.luxeperfume.backend.inventory.domain.InventoryQuantity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryQuantityTest {
    @Test
    void availableStockExcludesReservedUnits() {
        InventoryQuantity stock = new InventoryQuantity(20, 7, 3);
        assertThat(stock.availableQuantity()).isEqualTo(13);
        assertThat(stock.canReserve(13)).isTrue();
        assertThat(stock.canReserve(14)).isFalse();
    }

    @Test
    void reserveAndReleaseReturnNewValidatedStates() {
        InventoryQuantity original = new InventoryQuantity(20, 5, 2);
        InventoryQuantity reserved = original.reserve(8);
        InventoryQuantity released = reserved.release(4);

        assertThat(original.reserved()).isEqualTo(5);
        assertThat(reserved.reserved()).isEqualTo(13);
        assertThat(released.reserved()).isEqualTo(9);
        assertThat(released.availableQuantity()).isEqualTo(11);
    }

    @Test
    void adjustmentCannotConsumeReservedStockOrExceedDatabaseRange() {
        InventoryQuantity stock = new InventoryQuantity(10, 8, 1);

        assertThatThrownBy(() -> stock.adjustOnHand(-3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reserved quantity");
        assertThatThrownBy(() -> stock.adjustOnHand(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InventoryQuantity(5, 6, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InventoryQuantity(
                InventoryQuantity.MAX_SUPPORTED_QUANTITY + 1, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void releaseCannotExceedTheCurrentReservation() {
        InventoryQuantity stock = new InventoryQuantity(10, 3, 0);
        assertThatThrownBy(() -> stock.release(4))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stock.reserve(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
