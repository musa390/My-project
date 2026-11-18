package com.luxeperfume.backend.payment.domain;

import java.math.BigDecimal;

/** Provider adapter contract. Implementations must use sandbox endpoints and verify server-to-server. */
/** [ABS] Provider adapter contract; only officially specified sandbox adapters may implement it. */
public interface PaymentGateway {
    PaymentProvider provider();

    GatewayInitiationResult initiate(GatewayInitiationCommand command);

    GatewayVerificationResult verify(GatewayVerificationCommand command);

    record GatewayInitiationCommand(
            String merchantReference,
            BigDecimal amount,
            String currency,
            String callbackUrl,
            String customerEmail) {
        public GatewayInitiationCommand {
            if (merchantReference == null || merchantReference.isBlank()) throw new IllegalArgumentException("merchantReference is required");
            if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
            if (currency == null || !currency.matches("[A-Z]{3}")) throw new IllegalArgumentException("currency must be ISO-4217");
        }
    }

    record GatewayInitiationResult(
            String providerTransactionId,
            String providerReference,
            String redirectUrl,
            String providerStatus) {}

    record GatewayVerificationCommand(
            String merchantReference,
            String providerTransactionId) {}

    record GatewayVerificationResult(
            boolean verified,
            String merchantReference,
            String providerTransactionId,
            BigDecimal amount,
            String currency,
            String providerStatus,
            String verificationReference) {}
}
