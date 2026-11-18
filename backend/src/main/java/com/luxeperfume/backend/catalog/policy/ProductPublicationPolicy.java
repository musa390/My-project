package com.luxeperfume.backend.catalog.policy;

/** [POL] Product publication invariant, kept separate from HTTP and SQL concerns. */
public final class ProductPublicationPolicy {
    private ProductPublicationPolicy() {}

    /** A published product must have at least one active, purchasable variant. */
    public static void requirePublishable(int activeVariantCount) {
        if (activeVariantCount < 1) {
            throw new IllegalArgumentException("An active product must have at least one active variant");
        }
    }
}
