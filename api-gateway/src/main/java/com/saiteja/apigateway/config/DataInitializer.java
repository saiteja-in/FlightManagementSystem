package com.saiteja.apigateway.config;

import com.saiteja.apigateway.model.Role;
import com.saiteja.apigateway.model.enums.ERole;
import com.saiteja.apigateway.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        if (!roleRepository.findByName(ERole.USER).isPresent()) {
            Role userRole = new Role(ERole.USER);
            roleRepository.save(userRole);
            System.out.println("Initialized ROLE_USER");
        }

        if (!roleRepository.findByName(ERole.ADMIN).isPresent()) {
            Role adminRole = new Role(ERole.ADMIN);
            roleRepository.save(adminRole);
            System.out.println("Initialized ROLE_ADMIN");
        }
    }
}

