package com.luxeperfume.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.web.csrf.CsrfToken;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final boolean secure;
    private final long refreshDays;
    private final long accessMinutes;

    public AuthController(AuthService auth,
            @Value("${app.security.cookies.secure}") boolean secure,
            @Value("${app.security.jwt.refresh-days}") long refreshDays,
            @Value("${app.security.jwt.access-minutes}") long accessMinutes,
            @Value("${spring.profiles.active:}") String activeProfiles) {
        if (activeProfiles.contains("prod") && !secure) throw new IllegalStateException("Production auth cookies must be Secure.");
        this.auth = auth; this.secure = secure; this.refreshDays = refreshDays; this.accessMinutes = accessMinutes;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest body, HttpServletRequest request) {
        return sessionResponse(auth.register(body.email(), body.password(), body.firstName(), body.lastName(),
                request.getHeader("User-Agent"), request.getRemoteAddr()), true);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        return sessionResponse(auth.login(body.email(), body.password(), request.getHeader("User-Agent"),
                request.getRemoteAddr()), true);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name="lp_refresh", required=false) String token,
                                     HttpServletRequest request) {
        return sessionResponse(auth.refresh(token, request.getHeader("User-Agent"), request.getRemoteAddr()), true);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name="lp_refresh", required=false) String token) {
        auth.logout(token);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, clear("lp_access", "/api").toString())
                .header(HttpHeaders.SET_COOKIE, clear("lp_refresh", "/api/v1/auth").toString())
                .body(Map.of("success", true));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgot(@Valid @RequestBody EmailRequest body) {
        auth.forgotPassword(body.email());
        return ResponseEntity.accepted().body(Map.of("success", true,
                "message", "If the account exists, password reset instructions will be sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> reset(@Valid @RequestBody ResetRequest body) {
        auth.resetPassword(body.token(), body.newPassword());
        return ResponseEntity.ok(Map.of("success", true, "message", "Password updated. Please sign in again."));
    }

    private ResponseEntity<?> sessionResponse(AuthService.IssuedSession session, boolean includeRefresh) {
        ResponseCookie access = ResponseCookie.from("lp_access", session.accessToken())
                .httpOnly(true).secure(secure).sameSite("Lax").path("/api").maxAge(Duration.ofMinutes(accessMinutes)).build();
        ResponseCookie refresh = ResponseCookie.from("lp_refresh", session.refreshToken())
                .httpOnly(true).secure(secure).sameSite("Lax").path("/api/v1/auth")
                .maxAge(Duration.ofDays(refreshDays)).build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, access.toString())
                .header(HttpHeaders.SET_COOKIE, refresh.toString())
                .body(Map.of("success", true, "sessionId", session.sessionId()));
    }
    private ResponseCookie clear(String name, String path) {
        return ResponseCookie.from(name, "").httpOnly(true).secure(secure).sameSite("Lax")
                .path(path).maxAge(Duration.ZERO).build();
    }

    public record RegisterRequest(@Email @NotBlank String email, @NotBlank @Size(min=12,max=72) String password,
                                  @NotBlank @Size(max=100) String firstName, @Size(max=100) String lastName) {}
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record EmailRequest(@Email @NotBlank String email) {}
    public record ResetRequest(@NotBlank String token, @NotBlank @Size(min=12,max=72) String newPassword) {}
}
