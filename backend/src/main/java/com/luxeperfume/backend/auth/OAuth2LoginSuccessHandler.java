package com.luxeperfume.backend.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthService auth;
    private final boolean secure;
    private final long refreshDays;
    private final long accessMinutes;
    private final String redirectUrl;

    public OAuth2LoginSuccessHandler(AuthService auth,
            @Value("${app.security.cookies.secure}") boolean secure,
            @Value("${app.security.jwt.refresh-days}") long refreshDays,
            @Value("${app.security.jwt.access-minutes}") long accessMinutes,
            @Value("${app.security.oauth-success-redirect}") String redirectUrl,
            @Value("${spring.profiles.active:}") String activeProfiles) {
        if (activeProfiles.contains("prod") && !secure) throw new IllegalStateException("Production auth cookies must be Secure.");
        this.auth = auth; this.secure = secure; this.refreshDays = refreshDays; this.accessMinutes = accessMinutes; this.redirectUrl = redirectUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser user)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        AuthService.IssuedSession session = auth.loginGoogle(user, request.getHeader("User-Agent"),
                request.getRemoteAddr());
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("lp_access", session.accessToken())
                .httpOnly(true).secure(secure).sameSite("Lax").path("/api")
                .maxAge(Duration.ofMinutes(accessMinutes)).build().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("lp_refresh", session.refreshToken())
                .httpOnly(true).secure(secure).sameSite("Lax").path("/api/v1/auth")
                .maxAge(Duration.ofDays(refreshDays)).build().toString());
        response.sendRedirect(redirectUrl);
    }
}
