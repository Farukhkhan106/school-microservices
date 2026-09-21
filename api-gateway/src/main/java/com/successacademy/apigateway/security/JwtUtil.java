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
        Claims claims = getClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    public Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            try {
                // Fallback to legacy parser if parserBuilder is unavailable
                return Jwts.parser()
                        .setSigningKey(getKey())
                        .parseClaimsJws(token)
                        .getBody();
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
