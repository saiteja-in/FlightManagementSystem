package com.saiteja.apigateway.repository;

import com.saiteja.apigateway.model.Role;
import com.saiteja.apigateway.model.enums.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(ERole name);
}
