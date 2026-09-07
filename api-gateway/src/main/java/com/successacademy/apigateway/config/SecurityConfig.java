package com.successacademy.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    // ── Whitelisted frontend origins (production-ready CORS) ──────
    // Default "*" keeps local development simple.
    // PRODUCTION: set app.cors.allowed-origins=https://www.yourschool.com,https://yourschool.com
    // via environment variable or application.properties — then ONLY your
    // websites can call this API (blocks other sites from abusing it).
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public WebFilter corsWebFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            String origin = exchange.getRequest().getHeaders().getOrigin();

            if (allowedOrigins.trim().equals("*")) {
                headers.set("Access-Control-Allow-Origin", "*");
            } else if (origin != null && Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .anyMatch(o -> o.equalsIgnoreCase(origin))) {
                // Echo back the whitelisted origin
                headers.set("Access-Control-Allow-Origin", origin);
            }

            headers.set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH");
            headers.set("Access-Control-Allow-Headers", "Authorization,Content-Type,Accept,Origin");
            headers.set("Access-Control-Max-Age",       "3600");

            if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                exchange.getResponse().setStatusCode(HttpStatus.OK);
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        };
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(ex -> ex.anyExchange().permitAll());
        return http.build();
    }
}
