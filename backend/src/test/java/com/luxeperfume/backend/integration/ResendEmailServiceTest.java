package com.luxeperfume.backend.integration;

import com.luxeperfume.backend.integration.email.ResendEmailService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendEmailServiceTest {
    @Test
    void missingProviderKeyFailsExplicitlyInsteadOfSilentlyDroppingMail() {
        ResendEmailService service = new ResendEmailService(RestClient.builder(), "", "onboarding@resend.dev");
        assertThatThrownBy(() -> service.sendText("test@example.invalid", "subject", "body"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void sendsEmailThroughResendWithServerSideBearerToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-resend-key"))
                .andRespond(withSuccess("{\"id\":\"email_test_123\"}", MediaType.APPLICATION_JSON));

        ResendEmailService service = new ResendEmailService(builder, "test-resend-key", "verified@example.com");
        service.sendText("customer@example.invalid", "Order confirmed", "Thank you.");
        server.verify();
    }

    @Test
    void providerRejectionIsReturnedAsSanitizedDeliveryException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        ResendEmailService service = new ResendEmailService(builder, "test-resend-key", "verified@example.com");
        assertThatThrownBy(() -> service.sendText("customer@example.invalid", "subject", "body"))
                .isInstanceOf(ResendEmailService.EmailDeliveryException.class)
                .hasMessage("Email provider rejected the request");
        server.verify();
    }
}
