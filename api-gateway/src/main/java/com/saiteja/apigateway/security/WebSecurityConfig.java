package com.saiteja.apigateway.security;

import com.saiteja.apigateway.security.jwt.JwtAuthenticationConverter;
import com.saiteja.apigateway.security.jwt.ReactiveAuthenticationEntryPoint;
import com.saiteja.apigateway.security.oauth2.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class WebSecurityConfig {

    private final ReactiveAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final ReactiveUserDetailsService userDetailsService;
    private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    public WebSecurityConfig(ReactiveAuthenticationEntryPoint unauthorizedHandler,
                            JwtAuthenticationConverter jwtAuthenticationConverter,
                            ReactiveUserDetailsService userDetailsService,
                            @Lazy OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.userDetailsService = userDetailsService;
        this.oauth2AuthenticationSuccessHandler = oauth2AuthenticationSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager(PasswordEncoder passwordEncoder) {
        // Create password-based authentication manager for signin
        UserDetailsRepositoryReactiveAuthenticationManager passwordAuthManager =
                new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        passwordAuthManager.setPasswordEncoder(passwordEncoder);
        
        // Return a smart authentication manager that handles both cases:
        // 1. JWT tokens (has authorities, no credentials) - already validated, return as-is
        // 2. Username/password (has credentials, no authorities yet) - validate using password manager
        return authentication -> {
            // If authentication has authorities but no credentials, it's a JWT token
            // (JWT tokens are validated in JwtAuthenticationConverter and have authorities set)
            if (authentication.getCredentials() == null && 
                authentication.getAuthorities() != null && 
                !authentication.getAuthorities().isEmpty()) {
                // JWT token case - already validated, return as authenticated
                return Mono.just(authentication);
            }
            // Otherwise, it's username/password authentication - validate using password manager
            return passwordAuthManager.authenticate(authentication);
        };
    }


    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                          ReactiveAuthenticationManager authenticationManager) {
        // Create JWT authentication filter
        AuthenticationWebFilter jwtAuthenticationFilter = new AuthenticationWebFilter(authenticationManager);
        jwtAuthenticationFilter.setServerAuthenticationConverter(jwtAuthenticationConverter);
        jwtAuthenticationFilter.setRequiresAuthenticationMatcher(ServerWebExchangeMatchers.pathMatchers(
                "/api/v1.0/flight/admin/flights/**",
                "/api/v1.0/flight/admin/inventory",
                "/api/v1.0/flight/booking/**",
                "/api/v1.0/flight/ticket/**",
                "/api/v1.0/flight/bookings/**"
        ));

        http
                // Enable Spring Security CORS for direct endpoints (like /api/v1.0/auth/**)
                // Gateway CORS handles routed endpoints, Spring Security CORS handles direct endpoints
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints - no authentication required
                        .pathMatchers("/api/v1.0/auth/**").permitAll()
                        .pathMatchers("/health").permitAll()
                        .pathMatchers("/api/v1.0/flight/admin/internal/**").permitAll()
                        .pathMatchers("/api/v1.0/flight/admin/search").permitAll()
                        // OAuth2 endpoints
                        .pathMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // Protected endpoints - require authentication with specific roles
                        .pathMatchers("/api/v1.0/flight/admin/flights/**").hasAnyAuthority("ROLE_ADMIN")
                        .pathMatchers("/api/v1.0/flight/admin/inventory").hasAnyAuthority("ROLE_ADMIN")
                        .pathMatchers("/api/v1.0/flight/booking/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .pathMatchers("/api/v1.0/flight/ticket/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .pathMatchers("/api/v1.0/flight/bookings/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        // Allow all other requests to proceed (gateway will handle routing)
                        .anyExchange().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authenticationSuccessHandler(oauth2AuthenticationSuccessHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // When allowCredentials is true, cannot use wildcard (*) for allowedOrigins
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // Allow credentials - requests can include cookies
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply CORS config to all endpoints
        // This handles direct endpoints like /api/v1.0/auth/**
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

