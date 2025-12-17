package com.saiteja.apigateway.controller;

import com.saiteja.apigateway.dto.request.GoogleTokenRequest;
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
import java.util.Base64;

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

    @PostMapping("/oauth/google")
    public Mono<ResponseEntity<?>> authenticateGoogleUser(@Valid @RequestBody GoogleTokenRequest googleTokenRequest) {
        return Mono.fromCallable(() -> {
            // Decode Google ID token (JWT)
            String[] parts = googleTokenRequest.getIdToken().split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid Google ID token format");
            }
            
            // Decode the payload (second part)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            // Parse JSON payload (simplified - in production, use proper JSON parser)
            // Extract email, name, and sub from the payload
            String email = extractFromJson(payload, "email");
            String name = extractFromJson(payload, "name");
            String sub = extractFromJson(payload, "sub");
            
            if (email == null || sub == null) {
                throw new IllegalArgumentException("Invalid Google ID token: missing required fields");
            }
            
            return new String[]{email, name != null ? name : email.split("@")[0], sub};
        })
        .flatMap(userInfo -> {
            String email = userInfo[0];
            String name = userInfo[1];
            String providerId = userInfo[2];
            
            return authService.processOAuth2User(email, name, "google", providerId);
        })
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
            MessageResponse errorResponse = new MessageResponse("Error: " + e.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
        });
    }

    private String extractFromJson(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) {
                return null;
            }
            int valueStart = json.indexOf(":", keyIndex) + 1;
            // Skip whitespace and quotes
            while (valueStart < json.length() && (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '"')) {
                valueStart++;
            }
            int valueEnd = valueStart;
            while (valueEnd < json.length() && json.charAt(valueEnd) != '"' && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd).replace("\"", "");
        } catch (Exception e) {
            return null;
        }
    }
}

