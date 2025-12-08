package com.saiteja.apigateway.security.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationConverter implements ServerAuthenticationConverter {

    @Autowired
    private JwtUtils jwtUtils;
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);
        
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return Mono.empty();
        }

        String jwt = authHeader.substring(BEARER_PREFIX.length());
        
        if (!jwtUtils.validateJwtToken(jwt)) {
            return Mono.empty();
        }

        try {
            String username = jwtUtils.getUserNameFromJwtToken(jwt);
            List<SimpleGrantedAuthority> authorities = extractAuthoritiesFromToken(jwt);
            
            return Mono.just(new UsernamePasswordAuthenticationToken(username, null, authorities));
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private List<SimpleGrantedAuthority> extractAuthoritiesFromToken(String token) {
        try {
            io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parserBuilder()
                    .setSigningKey(jwtUtils.getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            List<String> roles = claims.get("roles", List.class);
            if (roles == null) {
                return new ArrayList<>();
            }
            
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority(role));
            }
            return authorities;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}

