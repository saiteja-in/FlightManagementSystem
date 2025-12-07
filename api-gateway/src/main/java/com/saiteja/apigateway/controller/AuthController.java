package com.saiteja.apigateway.controller;

import com.saiteja.apigateway.dto.request.LoginRequest;
import com.saiteja.apigateway.dto.request.SignupRequest;
import com.saiteja.apigateway.dto.response.MessageResponse;
import com.saiteja.apigateway.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1.0/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signin")
    public Mono<ResponseEntity<?>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.authenticate(loginRequest)
                .<ResponseEntity<?>>map(jwtResponse -> ResponseEntity.ok(jwtResponse))
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
        return Mono.just(ResponseEntity.ok(new MessageResponse("You've been signed out!")));
    }
}

