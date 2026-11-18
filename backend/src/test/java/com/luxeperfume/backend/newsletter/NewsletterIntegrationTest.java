package com.luxeperfume.backend.newsletter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class NewsletterIntegrationTest {
    @Autowired NewsletterController newsletter;
    @Autowired JdbcTemplate jdbc;

    @Test
    void unsubscribeTokenIsSingleUseAndOnlyItsSubscriberCanBeChanged() throws Exception {
        String email="newsletter-"+UUID.randomUUID()+"@example.invalid";
        String token="single-use-unsubscribe-token";
        jdbc.update("""
            INSERT INTO newsletter_subscribers(email,subscription_status,consented_at,confirmed_at,unsubscribe_token_hash)
            VALUES(?,'SUBSCRIBED',UTC_TIMESTAMP(),UTC_TIMESTAMP(),?)
            """,email,sha256(token));

        newsletter.unsubscribe(new NewsletterController.TokenRequest(token));
        String status=jdbc.queryForObject("SELECT subscription_status FROM newsletter_subscribers WHERE email=?",String.class,email);
        assertThat(status).isEqualTo("UNSUBSCRIBED");
        assertThatThrownBy(()->newsletter.unsubscribe(new NewsletterController.TokenRequest(token)))
                .isInstanceOf(ResponseStatusException.class);
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
