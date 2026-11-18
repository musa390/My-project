package com.luxeperfume.backend.newsletter;

import com.luxeperfume.backend.common.dto.ApiResponse;
import com.luxeperfume.backend.integration.email.ResendEmailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/v1/newsletter")
public class NewsletterController {
    private final JdbcTemplate jdbc;
    private final ResendEmailService email;
    private final String siteUrl;
    private final SecureRandom random=new SecureRandom();
    public NewsletterController(JdbcTemplate jdbc,ResendEmailService email,
            @Value("${app.storefront-url:http://localhost:3000}") String siteUrl) {
        this.jdbc=jdbc; this.email=email; this.siteUrl=siteUrl.replaceAll("/+$","");
    }
    @PostMapping("/subscribe")
    public ApiResponse<Map<String,Object>> subscribe(@Valid @RequestBody SubscribeRequest request) {
        String address=request.email().trim().toLowerCase();
        String token=token();
        jdbc.update("""
            INSERT INTO newsletter_subscribers(email,subscription_status,consented_at,confirmation_token_hash,
                confirmation_expires_at,consent_source)
            VALUES(?,'PENDING',UTC_TIMESTAMP(),?,DATE_ADD(UTC_TIMESTAMP(),INTERVAL 24 HOUR),?)
            ON DUPLICATE KEY UPDATE
                confirmation_token_hash=IF(subscription_status='SUBSCRIBED',confirmation_token_hash,VALUES(confirmation_token_hash)),
                confirmation_expires_at=IF(subscription_status='SUBSCRIBED',confirmation_expires_at,VALUES(confirmation_expires_at)),
                subscription_status=IF(subscription_status='SUBSCRIBED','SUBSCRIBED','PENDING'),
                consented_at=UTC_TIMESTAMP(),consent_source=VALUES(consent_source)
            """,address,sha256(token),request.source()==null?"website":request.source().trim());
        Integer subscribed=jdbc.queryForObject("SELECT COUNT(*) FROM newsletter_subscribers WHERE email=? AND subscription_status='SUBSCRIBED'",Integer.class,address);
        if(subscribed==null||subscribed==0) {
            try { email.sendText(address,"Confirm your LUXE PERFUME newsletter subscription",
                    "Confirm your subscription within 24 hours: "+siteUrl+"/api/v1/newsletter/confirm?token="+token); }
            catch(RuntimeException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Newsletter confirmation is temporarily unavailable."); }
        }
        return ApiResponse.success("If confirmation is required, an email will be sent.",Map.of("accepted",true));
    }
    @GetMapping("/confirm")
    public ApiResponse<Map<String,Object>> confirm(@RequestParam @NotBlank String token) {
        String tokenHash=sha256(token);
        var addresses=jdbc.query("SELECT email FROM newsletter_subscribers WHERE confirmation_token_hash=? AND confirmation_expires_at>UTC_TIMESTAMP() AND subscription_status='PENDING'",
                (rs,row)->rs.getString(1),tokenHash);
        if(addresses.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confirmation link is invalid or expired");
        String address=addresses.getFirst();
        String unsubscribe=token();
        int changed=jdbc.update("""
            UPDATE newsletter_subscribers SET subscription_status='SUBSCRIBED',confirmed_at=UTC_TIMESTAMP(),
                confirmation_token_hash=NULL,confirmation_expires_at=NULL,unsubscribe_token_hash=?
            WHERE confirmation_token_hash=? AND confirmation_expires_at>UTC_TIMESTAMP() AND subscription_status='PENDING'
            """,sha256(unsubscribe),tokenHash);
        if(changed!=1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confirmation link is invalid or expired");
        try {
            email.sendText(address,"Welcome to the LUXE PERFUME newsletter",
                    "Your subscription is confirmed. You can unsubscribe at any time: "
                            +siteUrl+"/api/v1/newsletter/unsubscribe?token="+unsubscribe);
        } catch(RuntimeException ex) {
            // Subscription remains active; the one-time unsubscribe token remains stored as a hash.
        }
        return ApiResponse.success("Newsletter subscription confirmed.",Map.of("subscribed",true,"unsubscribeToken",unsubscribe));
    }
    @GetMapping("/unsubscribe")
    public ApiResponse<Map<String,Object>> unsubscribeByLink(@RequestParam @NotBlank String token) {
        return unsubscribe(new TokenRequest(token));
    }
    @PostMapping("/unsubscribe")
    public ApiResponse<Map<String,Object>> unsubscribe(@Valid @RequestBody TokenRequest request) {
        int changed=jdbc.update("""
            UPDATE newsletter_subscribers SET subscription_status='UNSUBSCRIBED',
                unsubscribed_at=UTC_TIMESTAMP(),unsubscribe_token_hash=NULL
            WHERE unsubscribe_token_hash=? AND subscription_status='SUBSCRIBED'
            """,sha256(request.token()));
        if(changed!=1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsubscribe link is invalid or already used");
        return ApiResponse.success("Newsletter subscription cancelled.",Map.of("unsubscribed",true));
    }
    private String token(){byte[] b=new byte[32];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    private static String sha256(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    public record SubscribeRequest(@Email @NotBlank @Size(max=254) String email,@Size(max=80) String source){}
    public record TokenRequest(@NotBlank @Size(max=200) String token){}
}
