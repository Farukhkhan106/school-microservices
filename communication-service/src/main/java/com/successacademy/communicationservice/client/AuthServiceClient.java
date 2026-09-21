package com.successacademy.communicationservice.client;

import com.successacademy.communicationservice.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${auth.service.url:http://localhost:8084}")
    private String authServiceUrl;

    // Lightweight in-memory cache for user metadata to keep lookups fast
    private final Map<String, UserContext> cacheByUsername = new ConcurrentHashMap<>();
    private final Map<Long, UserContext> cacheById = new ConcurrentHashMap<>();

    public UserContext getUserByUsername(String username) {
        if (username == null || username.isBlank()) return null;
        String key = username.toLowerCase().trim();
        if (cacheByUsername.containsKey(key)) {
            return cacheByUsername.get(key);
        }

        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(authServiceUrl + "/auth/user/" + key, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                UserContext ctx = mapToContext(resp.getBody());
                if (ctx != null) {
                    cacheByUsername.put(key, ctx);
                    if (ctx.getUserId() != null) cacheById.put(ctx.getUserId(), ctx);
                    return ctx;
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch user by username '{}' from auth-service: {}", key, e.getMessage());
        }
        return null;
    }

    public UserContext getUserById(Long userId) {
        if (userId == null) return null;
        if (cacheById.containsKey(userId)) {
            return cacheById.get(userId);
        }

        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(authServiceUrl + "/auth/user-by-id/" + userId, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                UserContext ctx = mapToContext(resp.getBody());
                if (ctx != null) {
                    cacheById.put(userId, ctx);
                    if (ctx.getUsername() != null) cacheByUsername.put(ctx.getUsername().toLowerCase().trim(), ctx);
                    return ctx;
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch user by id '{}' from auth-service: {}", userId, e.getMessage());
        }
        return null;
    }

    private UserContext mapToContext(Map body) {
        Long id = toLong(body.get("id"));
        String uname = (String) body.get("username");
        String role = (String) body.get("role");
        Long sId = toLong(body.get("studentId"));
        Long tId = toLong(body.get("teacherId"));

        return UserContext.builder()
                .userId(id)
                .username(uname)
                .role(role != null ? role.toUpperCase() : "STUDENT")
                .studentId(sId)
                .teacherId(tId)
                .build();
    }

    private Long toLong(Object val) {
        if (val instanceof Number) return ((Number) val).longValue();
        if (val != null) {
            try { return Long.parseLong(val.toString()); } catch (Exception ignored) {}
        }
        return null;
    }
}
