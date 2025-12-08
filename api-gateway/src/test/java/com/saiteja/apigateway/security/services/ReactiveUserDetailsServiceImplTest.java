package com.saiteja.apigateway.security.services;

import com.saiteja.apigateway.model.ERole;
import com.saiteja.apigateway.model.Role;
import com.saiteja.apigateway.model.User;
import com.saiteja.apigateway.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveUserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReactiveUserDetailsServiceImpl service;

    @Test
    @DisplayName("findByUsername returns UserDetails when user exists")
    void findByUsernameSuccess() {
        User user = new User("alice", "alice@example.com", "pass");
        user.setId(5L);
        user.setRoles(Set.of(new Role(1, ERole.ROLE_USER)));

        when(userRepository.findByUsername(eq("alice"))).thenReturn(Optional.of(user));

        Mono<? extends org.springframework.security.core.userdetails.UserDetails> result = service.findByUsername("alice");

        StepVerifier.create(result)
                .assertNext(details -> {
                    assertThat(details.getUsername()).isEqualTo("alice");
                    assertThat(details.getAuthorities()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("findByUsername errors when user not found")
    void findByUsernameNotFound() {
        when(userRepository.findByUsername(eq("missing"))).thenReturn(Optional.empty());

        StepVerifier.create(service.findByUsername("missing"))
                .expectError(UsernameNotFoundException.class)
                .verify();
    }
}

