package com.saiteja.apigateway.repository;

import com.saiteja.apigateway.model.ERole;
import com.saiteja.apigateway.model.Role;
import com.saiteja.apigateway.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("existsByUsername and existsByEmail reflect persisted user")
    void existsChecksWork() {
        Role roleUser = roleRepository.save(new Role(null, ERole.ROLE_USER));

        User user = new User("alice", "alice@example.com", "pass123");
        user.setRoles(Set.of(roleUser));
        entityManager.persist(user);
        entityManager.flush();

        assertThat(userRepository.existsByUsername("alice")).isTrue();
        assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
        assertThat(userRepository.existsByUsername("bob")).isFalse();
        assertThat(userRepository.existsByEmail("bob@example.com")).isFalse();
    }

    @Test
    @DisplayName("findByUsername returns matching user with roles")
    void findByUsernameReturnsUser() {
        Role roleUser = roleRepository.save(new Role(null, ERole.ROLE_USER));
        User user = new User("bob", "bob@example.com", "pass123");
        user.setRoles(Set.of(roleUser));
        entityManager.persist(user);
        entityManager.flush();

        User fetched = userRepository.findByUsername("bob").orElseThrow();
        assertThat(fetched.getUsername()).isEqualTo("bob");
        assertThat(fetched.getRoles()).containsExactly(roleUser);
    }
}

