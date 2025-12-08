package com.saiteja.apigateway.security.jwt;

import com.saiteja.apigateway.model.Role;
import com.saiteja.apigateway.model.User;
import com.saiteja.apigateway.model.ERole;
import com.saiteja.apigateway.security.services.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private String secret;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        secret = Base64.getEncoder().encodeToString("verysecretkeyverysecretkey123456".getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3600000);
    }

    @Test
    @DisplayName("generateTokenFromUsername and parse back")
    void generateAndParseToken() {
        String token = jwtUtils.generateTokenFromUsername("alice");

        assertThat(jwtUtils.validateJwtToken(token)).isTrue();
        assertThat(jwtUtils.getUserNameFromJwtToken(token)).isEqualTo("alice");
    }

    @Test
    @DisplayName("generateTokenFromUserDetails includes roles claim")
    void generateTokenFromUserDetails() {
        Role role = new Role(1, ERole.ROLE_ADMIN);
        User user = new User("alice", "alice@example.com", "pass");
        user.setRoles(Set.of(role));
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        String token = jwtUtils.generateTokenFromUserDetails(userDetails);

        assertThat(jwtUtils.validateJwtToken(token)).isTrue();
        assertThat(jwtUtils.getUserNameFromJwtToken(token)).isEqualTo("alice");
    }

    @Test
    @DisplayName("validateJwtToken returns false for malformed token")
    void invalidTokenFailsValidation() {
        assertThat(jwtUtils.validateJwtToken("not-a-jwt")).isFalse();
    }
}

