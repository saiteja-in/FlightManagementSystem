package com.saiteja.apigateway.controller;

import com.saiteja.apigateway.dto.request.LoginRequest;
import com.saiteja.apigateway.dto.request.SignupRequest;
import com.saiteja.apigateway.dto.response.JwtResponse;
import com.saiteja.apigateway.dto.response.MessageResponse;
import com.saiteja.apigateway.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private WebTestClient webTestClient;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(authController)
                .configureClient()
                .baseUrl("/")
                .build();
    }

    @Test
    @DisplayName("signin: returns JWT on success")
    void signinReturnsJwtOnSuccess() {
        JwtResponse jwtResponse = new JwtResponse("token123", 1L, "alice", "alice@example.com", List.of("ROLE_USER"));

        when(authService.authenticate(any(LoginRequest.class))).thenReturn(Mono.just(jwtResponse));

        webTestClient.post()
                .uri("/api/v1.0/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"alice","password":"secret"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isEqualTo("token123")
                .jsonPath("$.username").isEqualTo("alice")
                .jsonPath("$.email").isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("signin: returns 401 on invalid credentials")
    void signinReturnsUnauthorizedOnFailure() {
        when(authService.authenticate(any(LoginRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("bad creds")));

        webTestClient.post()
                .uri("/api/v1.0/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"alice","password":"wrong"}
                        """)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Error: Invalid username or password!");
    }

    @Test
    @DisplayName("signup: returns 200 on success")
    void signupReturnsOkOnSuccess() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("bob");
        signupRequest.setEmail("bob@example.com");
        signupRequest.setPassword("pass123");
        signupRequest.setRole(Set.of("user"));

        when(authService.register(any(SignupRequest.class)))
                .thenReturn(Mono.just(new MessageResponse("User registered successfully!")));

        webTestClient.post()
                .uri("/api/v1.0/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"bob","email":"bob@example.com","password":"pass123","role":["user"]}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("User registered successfully!");
    }

    @Test
    @DisplayName("signup: returns 400 on duplicate user/email")
    void signupReturnsBadRequestOnDuplicate() {
        when(authService.register(any(SignupRequest.class)))
                .thenReturn(Mono.just(new MessageResponse("Error: Username is already taken!")));

        webTestClient.post()
                .uri("/api/v1.0/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username":"bob","email":"bob@example.com","password":"pass123","role":["user"]}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Error: Username is already taken!");
    }

    @Test
    @DisplayName("signout: returns ok")
    void signoutReturnsOk() {
        webTestClient.post()
                .uri("/api/v1.0/auth/signout")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("You've been signed out!");
    }

}

