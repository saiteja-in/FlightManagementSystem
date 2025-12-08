package com.saiteja.apigateway.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class JwtAuthenticationConverterTest {

    private JwtAuthenticationConverter converter;
    private JwtUtils jwtUtils;
    private Key signingKey;
    private String token;

    @BeforeEach
    void setUp() {
        jwtUtils = Mockito.mock(JwtUtils.class);
        converter = new JwtAuthenticationConverter();
        ReflectionTestUtils.setField(converter, "jwtUtils", jwtUtils);

        signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        token = Jwts.builder()
                .setSubject("alice")
                .claim("roles", List.of("ROLE_USER"))
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    @DisplayName("convert: returns Authentication on valid token")
    void convertReturnsAuthentication() {
        when(jwtUtils.validateJwtToken(token)).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken(token)).thenReturn("alice");
        when(jwtUtils.getSigningKey()).thenReturn(signingKey);

        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(converter.convert(exchange))
                .assertNext(auth -> {
                    assertThat(auth.getPrincipal()).isEqualTo("alice");
                    assertThat(auth.getAuthorities().stream().map(a -> a.getAuthority()))
                            .containsExactly("ROLE_USER");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("convert: returns empty when token invalid")
    void convertReturnsEmptyWhenInvalid() {
        when(jwtUtils.validateJwtToken(any())).thenReturn(false);

        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(converter.convert(exchange))
                .verifyComplete();
    }

    @Test
    @DisplayName("convert: returns empty when header missing")
    void convertReturnsEmptyWhenHeaderMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());

        StepVerifier.create(converter.convert(exchange))
                .verifyComplete();
    }
}

