package com.saiteja.apigateway.controller;

import com.saiteja.apigateway.dto.request.LoginRequest;
import com.saiteja.apigateway.dto.request.SignupRequest;
import com.saiteja.apigateway.dto.response.MessageResponse;
import com.saiteja.apigateway.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;

@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true", maxAge = 3600)
@RestController
@RequestMapping("/api/v1.0/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private static final String JWT_COOKIE_NAME = "jwt";
    private static final int COOKIE_MAX_AGE_DAYS = 7;

    @PostMapping("/signin")
    public Mono<ResponseEntity<?>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.authenticate(loginRequest)
                .<ResponseEntity<?>>map(jwtResponse -> {
                    // Create HttpOnly cookie with JWT token
                    ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE_NAME, jwtResponse.getToken())
                            .httpOnly(true)
                            .secure(false)  // Set to true in production with HTTPS
                            .sameSite("Strict")
                            .path("/")
                            .maxAge(Duration.ofDays(COOKIE_MAX_AGE_DAYS))
                            .build();

                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, cookie.toString())
                            .body(jwtResponse);
                })
                .onErrorResume(e -> {
                    MessageResponse errorResponse = new MessageResponse("Error: Invalid username or password!");
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
                });
    }

    @PostMapping("/signup")
    public Mono<ResponseEntity<?>> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        return authService.register(signUpRequest)
                .map(messageResponse -> {
                    if (messageResponse.getMessage().contains("Error")) {
                        return ResponseEntity.badRequest().body(messageResponse);
                    }
                    return ResponseEntity.ok(messageResponse);
                });
    }

    @PostMapping("/signout")
    public Mono<ResponseEntity<?>> logoutUser() {
        // Clear JWT cookie by setting maxAge to 0
        ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)  // Set to true in production with HTTPS
                .sameSite("Strict")
                .path("/")
                .maxAge(0)  // Delete cookie
                .build();

        return Mono.just(ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new MessageResponse("You've been signed out!")));
    }
}

