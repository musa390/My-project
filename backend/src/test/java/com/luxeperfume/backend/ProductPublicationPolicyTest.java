package com.luxeperfume.backend;

import com.luxeperfume.backend.catalog.policy.ProductPublicationPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductPublicationPolicyTest {
    @Test
    void productCanBePublishedOnlyWhenAtLeastOneVariantIsActive() {
        assertThatCode(() -> ProductPublicationPolicy.requirePublishable(1)).doesNotThrowAnyException();
        assertThatCode(() -> ProductPublicationPolicy.requirePublishable(4)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ProductPublicationPolicy.requirePublishable(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one active variant");
        assertThatThrownBy(() -> ProductPublicationPolicy.requirePublishable(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
