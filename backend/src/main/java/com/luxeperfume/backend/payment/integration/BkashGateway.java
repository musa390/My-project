package com.luxeperfume.backend.payment.integration;

import com.luxeperfume.backend.payment.domain.PaymentGateway;
import com.luxeperfume.backend.payment.domain.PaymentProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class BkashGateway implements PaymentGateway {
    private final RestClient client;
    private final boolean sandbox;
    private final String appKey;
    private final String appSecret;
    private final String username;
    private final String password;

    public BkashGateway(@Value("${app.payment.bkash.sandbox:true}") boolean sandbox,
                        @Value("${app.payment.bkash.app-key:}") String appKey,
                        @Value("${app.payment.bkash.app-secret:}") String appSecret,
                        @Value("${app.payment.bkash.username:}") String username,
                        @Value("${app.payment.bkash.password:}") String password) {
        this.client = RestClient.create();
        this.sandbox = sandbox;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.username = username;
        this.password = password;
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.BKASH;
    }

    @Override
    public GatewayInitiationResult initiate(GatewayInitiationCommand command) {
        requireCredentials();
        String baseUrl = sandbox
                ? "https://tokenized.sandbox.bka.sh/v1.2.0-beta"
                : "https://tokenized.pay.bka.sh/v1.2.0-beta";
        String token = getToken(baseUrl);
        Map<String, Object> created = client.post()
                .uri(baseUrl + "/checkout/create")
                .header("Authorization", token)
                .header("X-APP-Key", appKey)
                .header("X-APP-Secret", appSecret)
                .body(Map.of(
                        "mode", "0011",
                        "amount", command.amount().toPlainString(),
                        "currency", command.currency(),
                        "intent", "sale",
                        "merchantInvoiceNumber", command.merchantReference(),
                        "payerReference", command.customerEmail(),
                        "callbackURL", command.callbackUrl()))
                .retrieve()
                .body(Map.class);
        if (created == null || !"0000".equals(String.valueOf(created.get("statusCode")))) {
            throw new IllegalStateException("bKash create-payment failed");
        }
        return new GatewayInitiationResult(
                String.valueOf(created.get("paymentID")),
                command.merchantReference(),
                String.valueOf(created.get("bkashURL")),
                "PENDING");
    }

    @Override
    public GatewayVerificationResult verify(GatewayVerificationCommand command) {
        requireCredentials();
        String baseUrl = sandbox
                ? "https://tokenized.sandbox.bka.sh/v1.2.0-beta"
                : "https://tokenized.pay.bka.sh/v1.2.0-beta";
        String token = getToken(baseUrl);
        Map<String, Object> executed = client.post()
                .uri(baseUrl + "/checkout/execute")
                .header("Authorization", token)
                .header("X-APP-Key", appKey)
                .header("X-APP-Secret", appSecret)
                .body(Map.of("paymentID", command.providerTransactionId()))
                .retrieve()
                .body(Map.class);
        if (executed == null) throw new IllegalStateException("bKash execute returned no response");
        boolean completed = "0000".equals(String.valueOf(executed.get("statusCode")))
                && "Completed".equalsIgnoreCase(String.valueOf(executed.get("transactionStatus")));
        return new GatewayVerificationResult(
                completed,
                String.valueOf(executed.getOrDefault("merchantInvoiceNumber", command.merchantReference())),
                String.valueOf(executed.getOrDefault("paymentID", command.providerTransactionId())),
                new BigDecimal(String.valueOf(executed.getOrDefault("amount", "0"))),
                String.valueOf(executed.getOrDefault("currency", "BDT")),
                String.valueOf(executed.getOrDefault("transactionStatus", "FAILED")),
                String.valueOf(executed.getOrDefault("trxID", "")));
    }

    private String getToken(String baseUrl) {
        Map<String, Object> tokenResponse = client.post()
                .uri(baseUrl + "/checkout/token/grant")
                .header("username", username)
                .header("password", password)
                .header("X-APP-Key", appKey)
                .header("X-APP-Secret", appSecret)
                .body(Map.of())
                .retrieve()
                .body(Map.class);
        if (tokenResponse == null || tokenResponse.get("id_token") == null) {
            throw new IllegalStateException("bKash authorization token failed");
        }
        return String.valueOf(tokenResponse.get("id_token"));
    }

    private void requireCredentials() {
        if (appKey.isBlank() || appSecret.isBlank() || username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("bKash sandbox credentials are not configured");
        }
    }
}
