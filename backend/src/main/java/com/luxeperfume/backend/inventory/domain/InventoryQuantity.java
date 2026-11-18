package com.luxeperfume.backend.inventory.domain;

/**
 * [ENC] Immutable inventory state. Every quantity change is validated here so
 * application services cannot create negative availability or consume reserved stock.
 */
public final class InventoryQuantity {
    public static final long MAX_SUPPORTED_QUANTITY = 4_294_967_295L;

    private final long onHand;
    private final long reserved;
    private final long reorderLevel;

    public InventoryQuantity(long onHand, long reserved, long reorderLevel) {
        requireRange(onHand, "on-hand quantity");
        requireRange(reserved, "reserved quantity");
        requireRange(reorderLevel, "reorder level");
        if (reserved > onHand) {
            throw new IllegalArgumentException("Reserved quantity cannot exceed on-hand quantity");
        }
        this.onHand = onHand;
        this.reserved = reserved;
        this.reorderLevel = reorderLevel;
    }

    public long onHand() { return onHand; }
    public long reserved() { return reserved; }
    public long reorderLevel() { return reorderLevel; }
    public long availableQuantity() { return onHand - reserved; }

    public boolean canReserve(long quantity) {
        return quantity > 0 && quantity <= availableQuantity();
    }

    public InventoryQuantity reserve(long quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalArgumentException("Reservation exceeds available inventory");
        }
        return new InventoryQuantity(onHand, reserved + quantity, reorderLevel);
    }

    public InventoryQuantity release(long quantity) {
        if (quantity <= 0 || quantity > reserved) {
            throw new IllegalArgumentException("Release must be positive and cannot exceed reserved inventory");
        }
        return new InventoryQuantity(onHand, reserved - quantity, reorderLevel);
    }

    public InventoryQuantity adjustOnHand(long delta) {
        if (delta == 0) throw new IllegalArgumentException("Inventory adjustment cannot be zero");
        final long next;
        try {
            next = Math.addExact(onHand, delta);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("Adjusted stock exceeds the supported range", overflow);
        }
        if (next < reserved) {
            throw new IllegalArgumentException("Adjustment cannot reduce stock below reserved quantity (" + reserved + ")");
        }
        requireRange(next, "adjusted on-hand quantity");
        return new InventoryQuantity(next, reserved, reorderLevel);
    }

    private static void requireRange(long value, String label) {
        if (value < 0 || value > MAX_SUPPORTED_QUANTITY) {
            throw new IllegalArgumentException(label + " is outside the supported range");
        }
    }
}
