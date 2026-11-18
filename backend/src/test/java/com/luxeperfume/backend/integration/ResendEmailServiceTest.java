package com.luxeperfume.backend.integration;
import com.luxeperfume.backend.integration.email.ResendEmailService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class ResendEmailServiceTest {
    @Test void missingProviderKeyFailsExplicitlyInsteadOfSilentlyDroppingMail() {
        ResendEmailService service=new ResendEmailService("","onboarding@resend.dev");
        assertThatThrownBy(()->service.sendText("test@example.invalid","subject","body"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("not configured");
    }
}
