package com.example.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final ReactiveStringRedisTemplate redisTemplate;

    public AuthGlobalFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        boolean publicPath = isPublic(path, method);

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (publicPath) return chain.filter(exchange);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        return redisTemplate.hasKey("blacklist:" + token)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        if (publicPath) return chain.filter(exchange);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                    try {
                        Claims claims = parseToken(token);
                        Object userIdObj = claims.get("userId");
                        String userId = userIdObj != null ? String.valueOf(((Number) userIdObj).longValue()) : "";
                        ServerHttpRequest mutated = exchange.getRequest().mutate()
                                .header("X-User-Id", userId)
                                .header("X-User-Email", claims.getSubject())
                                .header("X-User-Role", claims.get("role", String.class))
                                .build();
                        return chain.filter(exchange.mutate().request(mutated).build());
                    } catch (JwtException | IllegalArgumentException e) {
                        if (publicPath) return chain.filter(exchange);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                });
    }

    private boolean isPublic(String path, String method) {
        if (path.startsWith("/auth/register") || path.startsWith("/auth/login")) return true;
        if (method.equals("GET") && path.startsWith("/categories")) return true;
        if (method.equals("GET") && (path.startsWith("/menus") || path.startsWith("/ai/recommend"))) return true;
        return false;
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
