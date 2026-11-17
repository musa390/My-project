package com.luxeperfume.backend.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void successResponseContainsExpectedValues() {
        ApiResponse<String> response = ApiResponse.success("OK", "ready");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("OK");
        assertThat(response.data()).isEqualTo("ready");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void failureResponseContainsExpectedValues() {
        ApiResponse<Void> response = ApiResponse.failure("Failed", null);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Failed");
        assertThat(response.data()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }
}
