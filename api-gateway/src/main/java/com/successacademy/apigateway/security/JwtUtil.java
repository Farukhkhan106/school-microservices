package com.successacademy.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
    private String secret;

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String validateToken(String token) {
        try {
            Claims claims = parse(token);
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .setSigningKey(getKey())
                .parseClaimsJws(token)
                .getBody();
    }

    /** Auth user id claim (null when absent in older tokens). */
    public Long extractUserId(String token) {
        try {
            Object v = parse(token).get("userId");
            return v instanceof Number ? ((Number) v).longValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Linked student id claim (null for non-students). */
    public Long extractStudentId(String token) {
        try {
            Object v = parse(token).get("studentId");
            return v instanceof Number ? ((Number) v).longValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Linked teacher id claim (null for non-teachers). */
    public Long extractTeacherId(String token) {
        try {
            Object v = parse(token).get("teacherId");
            return v instanceof Number ? ((Number) v).longValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            return parse(token).get("role", String.class);
        } catch (Exception e) {
            return null;
        }
    }
}
