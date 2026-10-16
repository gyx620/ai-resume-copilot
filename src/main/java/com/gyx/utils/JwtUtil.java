package com.gyx.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Creates and validates JWTs with the same Spring-managed signing key. */
@Component
public class JwtUtil {

    private static final long EXPIRE_MILLIS = 24 * 60 * 60 * 1000L;
    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("jwt.secret must not be empty");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(Long userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim("userId", userId)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + EXPIRE_MILLIS))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long getUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("userId", Long.class);
    }
}
