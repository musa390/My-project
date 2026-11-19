package com.luxeperfume.backend.payment.integration;

import com.luxeperfume.backend.payment.domain.PaymentGateway;
import com.luxeperfume.backend.payment.domain.PaymentProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class SslCommerzGateway implements PaymentGateway {
    private final RestClient client;
    private final boolean sandbox;
    private final String storeId;
    private final String storePassword;

    public SslCommerzGateway(@Value("${app.payment.sslcommerz.sandbox:true}") boolean sandbox,
                             @Value("${app.payment.sslcommerz.store-id:}") String storeId,
                             @Value("${app.payment.sslcommerz.store-password:}") String storePassword) {
        this.client = RestClient.create();
        this.sandbox = sandbox;
        this.storeId = storeId;
        this.storePassword = storePassword;
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.SSLCOMMERZ;
    }

    @Override
    public GatewayInitiationResult initiate(GatewayInitiationCommand command) {
        requireCredentials();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("store_id", storeId);
        form.add("store_passwd", storePassword);
        form.add("total_amount", command.amount().toPlainString());
        form.add("currency", command.currency());
        form.add("tran_id", command.merchantReference());
        form.add("success_url", command.callbackUrl() + "/success");
        form.add("fail_url", command.callbackUrl() + "/fail");
        form.add("cancel_url", command.callbackUrl() + "/cancel");
        form.add("ipn_url", command.callbackUrl() + "/ipn");
        form.add("cus_name", "LUXE Customer");
        form.add("cus_email", command.customerEmail());
        form.add("cus_phone", "01700000000");
        form.add("cus_add1", "Bangladesh");
        form.add("cus_city", "Dhaka");
        form.add("cus_country", "Bangladesh");
        form.add("shipping_method", "NO");
        form.add("product_name", "LUXE PERFUME Order");
        form.add("product_category", "Perfume");
        form.add("product_profile", "general");

        Map<String, Object> response = client.post()
                .uri(sandbox ? "https://sandbox.sslcommerz.com/gwprocess/v4/api.php"
                        : "https://securepay.sslcommerz.com/gwprocess/v4/api.php")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        if (response == null || !"SUCCESS".equalsIgnoreCase(String.valueOf(response.get("status")))) {
            throw new IllegalStateException("SSLCommerz session creation failed");
        }
        String sessionKey = String.valueOf(response.get("sessionkey"));
        String redirect = String.valueOf(response.get("GatewayPageURL"));
        return new GatewayInitiationResult(sessionKey, command.merchantReference(), redirect, "PENDING");
    }

    @Override
    public GatewayVerificationResult verify(GatewayVerificationCommand command) {
        requireCredentials();
        Map<String, Object> response = client.get()
                .uri(uri -> uri.scheme("https")
                        .host(sandbox ? "sandbox.sslcommerz.com" : "securepay.sslcommerz.com")
                        .path("/validator/api/validationserverAPI.php")
                        .queryParam("validation_id", command.providerTransactionId())
                        .queryParam("store_id", storeId)
                        .queryParam("store_passwd", storePassword)
                        .queryParam("v", "1")
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .body(Map.class);
        if (response == null) throw new IllegalStateException("SSLCommerz verification returned no response");
        String status = String.valueOf(response.get("status"));
        boolean verified = "VALID".equalsIgnoreCase(status) || "VALIDATED".equalsIgnoreCase(status);
        return new GatewayVerificationResult(
                verified,
                String.valueOf(response.getOrDefault("tran_id", command.merchantReference())),
                String.valueOf(response.getOrDefault("bank_tran_id", command.providerTransactionId())),
                new java.math.BigDecimal(String.valueOf(response.getOrDefault("amount", "0"))),
                String.valueOf(response.getOrDefault("currency", "BDT")),
                status,
                String.valueOf(response.getOrDefault("val_id", command.providerTransactionId())));
    }

    private void requireCredentials() {
        if (storeId.isBlank() || storePassword.isBlank()) {
            throw new IllegalStateException("SSLCommerz sandbox credentials are not configured");
        }
    }
}
