package com.successacademy.communicationservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;

@Service
public class JwtTokenService {

    @Value("${app.jwt.secret:MY_SUPER_SECRET_KEY_123456789012345}")
    private String secret;

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Claims parseToken(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token.trim())
                    .getBody();
        } catch (Exception e) {
            try {
                return Jwts.parser()
                        .setSigningKey(getKey())
                        .parseClaimsJws(token.trim())
                        .getBody();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public UserContext extractUserContext(String token) {
        Claims claims = parseToken(token);
        if (claims == null || claims.getSubject() == null) return null;

        String username = claims.getSubject();
        String role = claims.get("role", String.class);

        Long userId = null;
        Object uObj = claims.get("userId");
        if (uObj instanceof Number) {
            userId = ((Number) uObj).longValue();
        } else if (uObj != null) {
            try { userId = Long.parseLong(uObj.toString()); } catch (Exception ignored) {}
        }

        Long studentId = null;
        Object sObj = claims.get("studentId");
        if (sObj instanceof Number) {
            studentId = ((Number) sObj).longValue();
        } else if (sObj != null) {
            try { studentId = Long.parseLong(sObj.toString()); } catch (Exception ignored) {}
        }

        Long teacherId = null;
        Object tObj = claims.get("teacherId");
        if (tObj instanceof Number) {
            teacherId = ((Number) tObj).longValue();
        } else if (tObj != null) {
            try { teacherId = Long.parseLong(tObj.toString()); } catch (Exception ignored) {}
        }

        return UserContext.builder()
                .userId(userId)
                .username(username)
                .role(role != null ? role.toUpperCase() : "STUDENT")
                .studentId(studentId)
                .teacherId(teacherId)
                .build();
    }
}
