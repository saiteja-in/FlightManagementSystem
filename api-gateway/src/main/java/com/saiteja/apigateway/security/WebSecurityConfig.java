package com.saiteja.apigateway.security;

import com.saiteja.apigateway.security.jwt.JwtAuthenticationConverter;
import com.saiteja.apigateway.security.jwt.ReactiveAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class WebSecurityConfig {

    private final ReactiveAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final ReactiveUserDetailsService userDetailsService;

    public WebSecurityConfig(ReactiveAuthenticationEntryPoint unauthorizedHandler,
                            JwtAuthenticationConverter jwtAuthenticationConverter,
                            ReactiveUserDetailsService userDetailsService) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.userDetailsService = userDetailsService;
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
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints - no authentication required
                        .pathMatchers("/api/v1.0/auth/**").permitAll()
                        .pathMatchers("/health").permitAll()
                        .pathMatchers("/api/v1.0/flight/admin/internal/**").permitAll()
                        .pathMatchers("/api/v1.0/flight/admin/search").permitAll()
                        // Protected endpoints - require authentication with specific roles
                        .pathMatchers("/api/v1.0/flight/admin/flights/**").hasAnyAuthority("ROLE_ADMIN")
                        .pathMatchers("/api/v1.0/flight/admin/inventory").hasAnyAuthority("ROLE_ADMIN")
                        .pathMatchers("/api/v1.0/flight/booking/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .pathMatchers("/api/v1.0/flight/ticket/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .pathMatchers("/api/v1.0/flight/bookings/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        // Allow all other requests to proceed (gateway will handle routing)
                        .anyExchange().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable());

        return http.build();
    }
}

