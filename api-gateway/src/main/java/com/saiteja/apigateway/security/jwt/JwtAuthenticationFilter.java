package com.saiteja.apigateway.security.jwt;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String JWT_COOKIE_NAME = "jwt";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip processing for public endpoints - just forward
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        String jwt = parseJwt(request);
        
        // Always forward the Authorization header to downstream services if present
        if (StringUtils.hasText(jwt)) {
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header(AUTHORIZATION_HEADER, BEARER_PREFIX + jwt)
                    .build();
            
            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build();
            
            return chain.filter(modifiedExchange);
        }
        
        // No JWT token - forward as is (Spring Security will handle authorization)
        return chain.filter(exchange);
    }
    
    private String parseJwt(ServerHttpRequest request) {
        // Try to get token from cookie first (preferred method)
        String tokenFromCookie = getTokenFromCookie(request);
        if (StringUtils.hasText(tokenFromCookie)) {
            return tokenFromCookie;
        }
        
        // Fallback to Authorization header (for backward compatibility)
        String bearerToken = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
    
    private String getTokenFromCookie(ServerHttpRequest request) {
        if (request.getCookies() != null && request.getCookies().containsKey(JWT_COOKIE_NAME)) {
            var cookie = request.getCookies().getFirst(JWT_COOKIE_NAME);
            if (cookie != null && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/v1.0/auth/") ||
               path.equals("/health") ||
               path.startsWith("/api/v1.0/flight/admin/internal/") ||
               path.equals("/api/v1.0/flight/admin/search");
    }


    @Override
    public int getOrder() {
        // Run before Spring Security filters
        return -200;
    }
}

