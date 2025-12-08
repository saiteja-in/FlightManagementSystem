package com.saiteja.apigateway.service;

import com.saiteja.apigateway.dto.request.LoginRequest;
import com.saiteja.apigateway.dto.request.SignupRequest;
import com.saiteja.apigateway.dto.response.JwtResponse;
import com.saiteja.apigateway.dto.response.MessageResponse;
import com.saiteja.apigateway.model.ERole;
import com.saiteja.apigateway.model.Role;
import com.saiteja.apigateway.model.User;
import com.saiteja.apigateway.repository.RoleRepository;
import com.saiteja.apigateway.repository.UserRepository;
import com.saiteja.apigateway.security.jwt.JwtUtils;
import com.saiteja.apigateway.security.services.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private ReactiveAuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private Role roleUser;
    private Role roleAdmin;

    @BeforeEach
    void setUp() {
        roleUser = new Role(1, ERole.ROLE_USER);
        roleAdmin = new Role(2, ERole.ROLE_ADMIN);
    }

    @Test
    @DisplayName("register: assigns default USER role when no roles provided")
    void registerAssignsDefaultUserRole() {
        SignupRequest request = new SignupRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("secret");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(roleUser));
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        Mono<MessageResponse> result = authService.register(request);

        StepVerifier.create(result)
                .assertNext(res -> assertThat(res.getMessage()).isEqualTo("User registered successfully!"))
                .verifyComplete();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRoles()).containsExactly(roleUser);
    }

    @Test
    @DisplayName("register: returns duplicate username message")
    void registerDuplicateUsername() {
        SignupRequest request = new SignupRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("secret");

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        StepVerifier.create(authService.register(request))
                .assertNext(res -> assertThat(res.getMessage()).isEqualTo("Error: Username is already taken!"))
                .verifyComplete();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: returns duplicate email message")
    void registerDuplicateEmail() {
        SignupRequest request = new SignupRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("secret");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        StepVerifier.create(authService.register(request))
                .assertNext(res -> assertThat(res.getMessage()).isEqualTo("Error: Email is already in use!"))
                .verifyComplete();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: assigns ADMIN role when requested")
    void registerAssignsAdminRole() {
        SignupRequest request = new SignupRequest();
        request.setUsername("admin");
        request.setEmail("admin@example.com");
        request.setPassword("secret");
        request.setRole(new HashSet<>(Set.of("admin")));

        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(roleAdmin));
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(authService.register(request))
                .assertNext(res -> assertThat(res.getMessage()).isEqualTo("User registered successfully!"))
                .verifyComplete();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRoles()).containsExactly(roleAdmin);
    }

    @Test
    @DisplayName("authenticate: returns JwtResponse on success")
    void authenticateReturnsJwtOnSuccess() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("alice");
        loginRequest.setPassword("secret");

        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "alice", "alice@example.com", "encoded",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(Mono.just(authentication));
        when(jwtUtils.generateTokenFromUserDetails(userDetails)).thenReturn("jwt123");

        Mono<JwtResponse> result = authService.authenticate(loginRequest);

        StepVerifier.create(result)
                .assertNext(jwt -> {
                    assertThat(jwt.getToken()).isEqualTo("jwt123");
                    assertThat(jwt.getUsername()).isEqualTo("alice");
                    assertThat(jwt.getEmail()).isEqualTo("alice@example.com");
                    assertThat(jwt.getRoles()).containsExactly("ROLE_USER");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("authenticate: propagates error on bad credentials")
    void authenticatePropagatesError() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("alice");
        loginRequest.setPassword("wrong");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(Mono.error(new RuntimeException("bad creds")));

        StepVerifier.create(authService.authenticate(loginRequest))
                .expectErrorMessage("bad creds")
                .verify();
    }
}

