package com.luxeperfume.backend.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final String issuer;
    private final long accessMinutes;

    public JwtService(
            @Value("${app.security.jwt.secret-base64}") String secret,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.access-minutes}") long accessMinutes) {
        this.key = Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(secret));
        this.issuer = issuer;
        this.accessMinutes = accessMinutes;
    }

    public String issue(long userId, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(Long.toString(userId))
                .claim("sid", sessionId)
                .claim("purpose", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessMinutes * 60)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).requireIssuer(issuer).build()
                .parseSignedClaims(token).getPayload();
    }
}
