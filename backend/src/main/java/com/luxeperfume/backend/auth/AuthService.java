package com.luxeperfume.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final SecureRandom random = new SecureRandom();
    private final long refreshDays;
    private final boolean secureCookies;
    private final String resetUrl;
    private final String resendKey;
    private final String emailFrom;

    public AuthService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, JwtService jwt,
            @Value("${app.security.jwt.refresh-days}") long refreshDays,
            @Value("${app.security.cookies.secure}") boolean secureCookies,
            @Value("${app.security.password-reset.frontend-url}") String resetUrl,
            @Value("${app.security.email.resend-api-key}") String resendKey,
            @Value("${app.security.email.from}") String emailFrom) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.refreshDays = refreshDays;
        this.secureCookies = secureCookies;
        this.resetUrl = resetUrl;
        this.resendKey = resendKey;
        this.emailFrom = emailFrom;
    }

    @Transactional
    public IssuedSession register(String email, String password, String firstName, String lastName,
                                 String userAgent, String ip) {
        String normalized = email.trim().toLowerCase();
        if (password.length() < 12 || password.length() > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be 12-72 characters");
        try {
            KeyHolder key = new GeneratedKeyHolder();
            jdbc.update((Connection c) -> {
                PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users(email,password_hash,first_name,last_name,auth_provider,account_status) VALUES(?,?,?,?,'LOCAL','ACTIVE')",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, normalized);
                ps.setString(2, passwordEncoder.encode(password));
                ps.setString(3, firstName.trim());
                ps.setString(4, lastName == null ? null : lastName.trim());
                return ps;
            }, key);
            long userId = key.getKey().longValue();
            jdbc.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code='CUSTOMER'", userId);
            return createSession(userId, userAgent, ip);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
    }

    @Transactional
    public IssuedSession login(String email, String password, String userAgent, String ip) {
        List<Map<String,Object>> users = jdbc.queryForList(
                "SELECT id,password_hash,account_status FROM users WHERE email=? AND auth_provider='LOCAL' AND deleted_at IS NULL",
                email.trim().toLowerCase());
        if (users.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        Map<String,Object> user = users.getFirst();
        String status = (String) user.get("account_status");
        String hash = (String) user.get("password_hash");
        if (!"ACTIVE".equals(status) || hash == null || !passwordEncoder.matches(password, hash))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        long userId = ((Number) user.get("id")).longValue();
        jdbc.update("UPDATE users SET last_login_at=UTC_TIMESTAMP() WHERE id=?", userId);
        return createSession(userId, userAgent, ip);
    }

    @Transactional
    public IssuedSession loginGoogle(OidcUser oidcUser, String userAgent, String ip) {
        Boolean verified = oidcUser.getClaim("email_verified");
        String email = oidcUser.getEmail();
        String subject = oidcUser.getSubject();
        if (email == null || !Boolean.TRUE.equals(verified) || subject == null || subject.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google account email must be verified");

        List<Long> existing = jdbc.query(
                "SELECT id FROM users WHERE auth_provider='GOOGLE' AND provider_subject=? AND deleted_at IS NULL",
                (rs, n) -> rs.getLong(1), subject);
        long userId;
        if (!existing.isEmpty()) {
            userId = existing.getFirst();
        } else {
            Integer emailTaken = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email=?", Integer.class,
                    email.trim().toLowerCase());
            if (emailTaken != null && emailTaken > 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "An account already exists for this email. Sign in using its original method.");
            KeyHolder key = new GeneratedKeyHolder();
            jdbc.update((Connection c) -> {
                PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users(email,password_hash,first_name,last_name,auth_provider,provider_subject,account_status,email_verified_at) VALUES(?,NULL,?,?, 'GOOGLE',?,'ACTIVE',UTC_TIMESTAMP())",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, email.trim().toLowerCase());
                ps.setString(2, oidcUser.getGivenName() == null ? "Google" : oidcUser.getGivenName());
                ps.setString(3, oidcUser.getFamilyName());
                ps.setString(4, subject);
                return ps;
            }, key);
            userId = key.getKey().longValue();
            jdbc.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code='CUSTOMER'", userId);
        }
        return createSession(userId, userAgent, ip);
    }

    @Transactional
    public IssuedSession refresh(String refreshToken, String userAgent, String ip) {
        if (refreshToken == null || refreshToken.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String oldHash = sha256(refreshToken);
        List<Map<String,Object>> rows = jdbc.queryForList(
                "SELECT id,user_id,expires_at,revoked_at FROM auth_sessions WHERE refresh_token_hash=? FOR UPDATE", oldHash);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        Map<String,Object> row = rows.getFirst();
        if (row.get("revoked_at") != null || !((LocalDateTime) row.get("expires_at")).isAfter(LocalDateTime.now()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String sessionId = (String) row.get("id");
        long userId = ((Number) row.get("user_id")).longValue();
        String next = randomToken();
        jdbc.update("UPDATE auth_sessions SET refresh_token_hash=?,expires_at=DATE_ADD(UTC_TIMESTAMP(),INTERVAL ? DAY),last_used_at=UTC_TIMESTAMP(),user_agent=?,ip_address=? WHERE id=?",
                sha256(next), refreshDays, userAgent, ip, sessionId);
        return new IssuedSession(jwt.issue(userId, sessionId), next, sessionId);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank())
            jdbc.update("UPDATE auth_sessions SET revoked_at=UTC_TIMESTAMP() WHERE refresh_token_hash=? AND revoked_at IS NULL",
                    sha256(refreshToken));
    }

    @Transactional
    public void forgotPassword(String email) {
        List<Long> ids = jdbc.query("SELECT id FROM users WHERE email=? AND auth_provider='LOCAL' AND account_status='ACTIVE' AND deleted_at IS NULL",
                (rs, n) -> rs.getLong(1), email.trim().toLowerCase());
        if (ids.isEmpty()) return;
        long userId = ids.getFirst();
        String token = randomToken();
        jdbc.update("UPDATE password_reset_tokens SET used_at=UTC_TIMESTAMP() WHERE user_id=? AND used_at IS NULL", userId);
        jdbc.update("INSERT INTO password_reset_tokens(user_id,token_hash,expires_at) VALUES(?,?,DATE_ADD(UTC_TIMESTAMP(),INTERVAL 30 MINUTE))",
                userId, sha256(token));
        if (resendKey != null && !resendKey.isBlank()) {
            String link = resetUrl + (resetUrl.contains("?") ? "&" : "?") + "token=" + token;
            RestClient.create("https://api.resend.com")
                    .post().uri("/emails")
                    .header("Authorization", "Bearer " + resendKey)
                    .body(Map.of("from", emailFrom, "to", List.of(email.trim()),
                            "subject", "Reset your LUXE PERFUME password",
                            "text", "Use this one-time link within 30 minutes: " + link))
                    .retrieve().toBodilessEntity();
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (newPassword.length() < 12 || newPassword.length() > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be 12-72 characters");
        List<Map<String,Object>> rows = jdbc.queryForList(
                "SELECT id,user_id,expires_at,used_at FROM password_reset_tokens WHERE token_hash=? FOR UPDATE",
                sha256(token));
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token");
        Map<String,Object> row = rows.getFirst();
        if (row.get("used_at") != null || !((LocalDateTime) row.get("expires_at")).isAfter(LocalDateTime.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token");
        long userId = ((Number) row.get("user_id")).longValue();
        jdbc.update("UPDATE users SET password_hash=? WHERE id=?", passwordEncoder.encode(newPassword), userId);
        jdbc.update("UPDATE password_reset_tokens SET used_at=UTC_TIMESTAMP() WHERE id=?", row.get("id"));
        jdbc.update("UPDATE auth_sessions SET revoked_at=UTC_TIMESTAMP() WHERE user_id=? AND revoked_at IS NULL", userId);
    }

    private IssuedSession createSession(long userId, String userAgent, String ip) {
        String id = UUID.randomUUID().toString();
        String refresh = randomToken();
        jdbc.update("INSERT INTO auth_sessions(id,user_id,refresh_token_hash,expires_at,user_agent,ip_address) VALUES(?,?,?,DATE_ADD(UTC_TIMESTAMP(),INTERVAL ? DAY),?,?)",
                id, userId, sha256(refresh), refreshDays, userAgent, ip);
        return new IssuedSession(jwt.issue(userId, id), refresh, id);
    }

    private String randomToken() {
        byte[] bytes = new byte[48]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    public record IssuedSession(String accessToken, String refreshToken, String sessionId) {}
}
