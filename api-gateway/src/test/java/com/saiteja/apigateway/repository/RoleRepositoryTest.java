package com.saiteja.apigateway.repository;

import com.saiteja.apigateway.model.ERole;
import com.saiteja.apigateway.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RoleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("findByName returns persisted role")
    void findByNameReturnsRole() {
        Role adminRole = new Role(null, ERole.ROLE_ADMIN);
        entityManager.persist(adminRole);
        entityManager.flush();

        Role found = roleRepository.findByName(ERole.ROLE_ADMIN).orElseThrow();
        assertThat(found.getName()).isEqualTo(ERole.ROLE_ADMIN);
    }
}

