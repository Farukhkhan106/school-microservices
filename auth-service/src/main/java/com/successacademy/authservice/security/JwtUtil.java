package com.successacademy.authservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // ⭐ Production-ready: secret comes from environment / application.properties.
    //    Default keeps local development working — CHANGE IT on the live server
    //    (MUST be the same value in api-gateway AND auth-service!)
    @Value("${app.jwt.secret:MY_SUPER_SECRET_KEY_123456789012345}")
    private String secret; // 32+ chars

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    private final long EXPIRATION = 1000 * 60 * 60 * 24; // 24 hours

    // CREATE TOKEN
    public String generateToken(String username, String role) {
        return generateToken(username, role, null, null);
    }

    // Full token — carries the authenticated user's id (and linked student / teacher id)
    // so downstream services (via gateway headers) can authorize ownership.
    public String generateToken(String username, String role, Long userId, Long studentId) {
        return generateToken(username, role, userId, studentId, null);
    }

    public String generateToken(String username, String role, Long userId, Long studentId, Long teacherId) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("userId", userId)
                .claim("studentId", studentId)
                .claim("teacherId", teacherId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // VALIDATE TOKEN
    public String validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public String extractRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }
}
