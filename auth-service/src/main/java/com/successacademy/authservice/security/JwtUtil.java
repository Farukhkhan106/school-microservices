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

    // CREATE TOKEN (Overload with userId, studentId, teacherId)
    public String generateToken(String username, String role, Long userId, Long studentId, Long teacherId) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION));

        if (studentId != null) {
            builder.claim("studentId", studentId);
        }
        if (teacherId != null) {
            builder.claim("teacherId", teacherId);
        }

        return builder.signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
    }

    // Legacy overload
    public String generateToken(String username, String role) {
        return generateToken(username, role, null, null, null);
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

    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public String extractRole(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.get("role", String.class) : null;
    }
}
