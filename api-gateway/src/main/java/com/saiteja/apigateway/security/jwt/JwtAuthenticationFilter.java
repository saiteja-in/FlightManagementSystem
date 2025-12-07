package com.saiteja.apigateway.security.jwt;

import com.saiteja.apigateway.security.services.ReactiveUserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ReactiveUserDetailsServiceImpl userDetailsService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        String jwt = parseJwt(request);
        
        if (StringUtils.hasText(jwt) && jwtUtils.validateJwtToken(jwt)) {
            String username = jwtUtils.getUserNameFromJwtToken(jwt);
            
            return userDetailsService.findByUsername(username)
                    .flatMap(userDetails -> {
                        List<SimpleGrantedAuthority> authorities = userDetails.getAuthorities().stream()
                                .map(auth -> new SimpleGrantedAuthority(auth.getAuthority()))
                                .collect(Collectors.toList());
                        
                        UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                        
                        SecurityContext securityContext = new SecurityContextImpl(authentication);
                        
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
                    })
                    .onErrorResume(e -> {
                        logger.error("Cannot set user authentication: {}", e.getMessage());
                        // Let Spring Security handle unauthorized - just continue the chain
                        return chain.filter(exchange);
                    });
        } else {
            // No valid token - let Spring Security handle authorization
            // It will check if the endpoint requires authentication
            return chain.filter(exchange);
        }
    }

    private String parseJwt(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
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
        return -100;
    }
}

