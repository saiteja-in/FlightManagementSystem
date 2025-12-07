package com.saiteja.apigateway.security;

import com.saiteja.apigateway.security.jwt.ReactiveAuthenticationEntryPoint;
import com.saiteja.apigateway.security.services.ReactiveUserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class WebSecurityConfig {

    @Autowired
    private ReactiveUserDetailsServiceImpl userDetailsService;

    @Autowired
    private ReactiveAuthenticationEntryPoint unauthorizedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        UserDetailsRepositoryReactiveAuthenticationManager authenticationManager =
                new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        authenticationManager.setPasswordEncoder(passwordEncoder());
        return authenticationManager;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1.0/auth/**").permitAll()
                        .pathMatchers("/health").permitAll()
                        .pathMatchers("/api/v1.0/flight/admin/internal/**").permitAll()
                        .pathMatchers("/api/v1.0/flight/admin/search").permitAll()
                        .pathMatchers("/api/v1.0/flight/admin/flights/**").hasRole("ADMIN")
                        .pathMatchers("/api/v1.0/flight/admin/inventory").hasRole("ADMIN")
                        .pathMatchers("/api/v1.0/flight/booking/**").hasAnyRole("USER", "ADMIN")
                        .pathMatchers("/api/v1.0/flight/ticket/**").hasAnyRole("USER", "ADMIN")
                        .pathMatchers("/api/v1.0/flight/bookings/**").hasAnyRole("USER", "ADMIN")
                        .anyExchange().authenticated()
                )
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance());

        return http.build();
    }
}

