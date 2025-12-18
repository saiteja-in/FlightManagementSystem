package com.saiteja.apigateway.security.oauth2;

import com.saiteja.apigateway.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final AuthService authService;
    private static final String JWT_COOKIE_NAME = "jwt";
    private static final int COOKIE_MAX_AGE_DAYS = 7;
    private static final String FRONTEND_URL = "http://localhost:4200";

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        ServerHttpResponse response = exchange.getResponse();
        
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauth2User = oauth2Token.getPrincipal();
            
            String provider = oauth2Token.getAuthorizedClientRegistrationId();
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");
            String providerId = oauth2User.getName(); // Usually the user ID from the provider
            
            if (name == null) {
                name = email != null ? email.split("@")[0] : "user";
            }
            
            if (email == null) {
                email = providerId + "@" + provider + ".local";
            }
            
            return authService.processOAuth2User(email, name, provider, providerId)
                    .flatMap(jwtResponse -> {
                        response.setStatusCode(HttpStatus.FOUND);
                        String rolesParam = String.join(",", jwtResponse.getRoles());
                        response.getHeaders().set(HttpHeaders.LOCATION, 
                                FRONTEND_URL + "/oauth2/callback?token=" + 
                                URLEncoder.encode(jwtResponse.getToken(), StandardCharsets.UTF_8) +
                                "&id=" + jwtResponse.getId() +
                                "&username=" + URLEncoder.encode(jwtResponse.getUsername(), StandardCharsets.UTF_8) +
                                "&email=" + URLEncoder.encode(jwtResponse.getEmail(), StandardCharsets.UTF_8) +
                                "&roles=" + URLEncoder.encode(rolesParam, StandardCharsets.UTF_8));
                        
                        // Also set JWT in cookie
                        ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE_NAME, jwtResponse.getToken())
                                .httpOnly(true)
                                .secure(false)  // Set to true in production with HTTPS
                                .sameSite("Strict")
                                .path("/")
                                .maxAge(Duration.ofDays(COOKIE_MAX_AGE_DAYS))
                                .build();
                        response.getHeaders().add(HttpHeaders.SET_COOKIE, cookie.toString());
                        
                        return response.setComplete();
                    })
                    .onErrorResume(e -> {
                        response.setStatusCode(HttpStatus.FOUND);
                        response.getHeaders().set(HttpHeaders.LOCATION, 
                                FRONTEND_URL + "/login?error=" + 
                                URLEncoder.encode("OAuth2 authentication failed: " + e.getMessage(), StandardCharsets.UTF_8));
                        return response.setComplete();
                    });
        }
        
        // Fallback: redirect to frontend
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().set(HttpHeaders.LOCATION, FRONTEND_URL + "/login?error=Invalid authentication");
        return response.setComplete();
    }
}

