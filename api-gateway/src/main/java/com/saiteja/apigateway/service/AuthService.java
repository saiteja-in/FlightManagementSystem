package com.saiteja.apigateway.service;

import com.saiteja.apigateway.dto.JwtResponse;
import com.saiteja.apigateway.dto.LoginRequest;
import com.saiteja.apigateway.dto.MessageResponse;
import com.saiteja.apigateway.dto.SignUpRequest;
import com.saiteja.apigateway.model.Role;
import com.saiteja.apigateway.model.User;
import com.saiteja.apigateway.model.enums.ERole;
import com.saiteja.apigateway.repository.RoleRepository;
import com.saiteja.apigateway.repository.UserRepository;
import com.saiteja.apigateway.security.jwt.JwtUtils;
import com.saiteja.apigateway.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ReactiveAuthenticationManager authenticationManager;

    public Mono<MessageResponse> register(SignUpRequest signUpRequest) {
        return Mono.fromCallable(() -> {
            if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                return new MessageResponse("Error: Username is already taken!");
            }

            if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                return new MessageResponse("Error: Email is already in use!");
            }

            // create new user account
            User user = new User(signUpRequest.getUsername(),
                    signUpRequest.getEmail(),
                    passwordEncoder.encode(signUpRequest.getPassword()));

            Set<String> strRoles = signUpRequest.getRole();
            Set<Role> roles = new HashSet<>();

            if (strRoles == null || strRoles.isEmpty()) {
                Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                        .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                roles.add(userRole);
            } else {
                strRoles.forEach(role -> {
                    switch (role.toLowerCase()) {
                        case "admin":
                            Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                            roles.add(adminRole);
                            break;
                        default:
                            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                            roles.add(userRole);
                    }
                });
            }

            user.setRoles(roles);
            userRepository.save(user);

            return new MessageResponse("User registered successfully!");
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<JwtResponse> authenticate(LoginRequest loginRequest) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());

        return authenticationManager.authenticate(authenticationToken)
                .flatMap(authentication -> {
                    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                    String jwt = jwtUtils.generateTokenFromUserDetails(userDetails);
                    List<String> roles = userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList());

                    return Mono.just(new JwtResponse(jwt,
                            userDetails.getId(),
                            userDetails.getUsername(),
                            userDetails.getEmail(),
                            roles));
                });
    }
}
