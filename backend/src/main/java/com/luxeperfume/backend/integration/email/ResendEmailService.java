package com.luxeperfume.backend.integration.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import java.util.List;
import java.util.Map;

@Service
public class ResendEmailService {
    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);
    private final RestClient client;
    private final String apiKey;
    private final String from;

    public ResendEmailService(@Value("${app.security.email.resend-api-key:}") String apiKey,
            @Value("${app.security.email.from:onboarding@resend.dev}") String from) {
        this.client = RestClient.builder().baseUrl("https://api.resend.com").build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.from = from;
    }
    public boolean isConfigured() { return !apiKey.isBlank(); }
    public void sendText(String to, String subject, String text) { send(to, subject, text, null); }

    public void send(String to, String subject, String text, String html) {
        if (!isConfigured()) throw new IllegalStateException("Resend email provider is not configured");
        if (to == null || to.isBlank() || subject == null || subject.isBlank())
            throw new IllegalArgumentException("Recipient and subject are required");
        Map<String, Object> payload = html == null || html.isBlank()
                ? Map.of("from", from, "to", List.of(to), "subject", subject, "text", text)
                : Map.of("from", from, "to", List.of(to), "subject", subject, "text", text, "html", html);
        try {
            client.post().uri("/emails").contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey).body(payload).retrieve().toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.error("Resend rejected an email request (HTTP {}).", ex.getStatusCode().value());
            throw new EmailDeliveryException("Email provider rejected the request", ex);
        } catch (RuntimeException ex) {
            log.error("Resend email delivery failed due to a provider/network error.");
            throw new EmailDeliveryException("Email delivery failed", ex);
        }
    }
    public static class EmailDeliveryException extends RuntimeException {
        public EmailDeliveryException(String message, Throwable cause) { super(message, cause); }
    }
}
