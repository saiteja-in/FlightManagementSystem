package com.saiteja.apigateway.security.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
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

        // Extract JWT token from cookie or header
        String jwt = parseJwt(request);
        
        // Always forward the Authorization header to downstream services if JWT token is found
        // This ensures downstream services (like booking-service) receive the token
        if (StringUtils.hasText(jwt)) {
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header(AUTHORIZATION_HEADER, BEARER_PREFIX + jwt)
                    .build();
            
            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build();
            
            logger.debug("Added Authorization header for path: {}", path);
            return chain.filter(modifiedExchange);
        }
        
        // Check if Authorization header already exists (from frontend or previous filter)
        // If it exists, forward as is
        String existingAuth = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(existingAuth) && existingAuth.startsWith(BEARER_PREFIX)) {
            logger.debug("Authorization header already exists for path: {}", path);
            return chain.filter(exchange);
        }
        
        // No JWT token found - log for debugging
        logger.warn("No JWT token found for protected path: {}", path);
        
        // Forward as is (Spring Security will handle authorization and reject if needed)
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
        // Run before Spring Security filters to add Authorization header
        // This ensures the header is available for both Spring Security and downstream services
        return -200;
    }
}

