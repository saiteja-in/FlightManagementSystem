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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.SecureRandom;
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

    public Mono<MessageResponse> register(SignupRequest signUpRequest) {
        return Mono.fromCallable(() -> {
            if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                return new MessageResponse("Error: Username is already taken!");
            }

            if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                return new MessageResponse("Error: Email is already in use!");
            }

            // Create new user's account
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

    public Mono<JwtResponse> processOAuth2User(String email, String name, String provider, String providerId) {
        return Mono.fromCallable(() -> {
            // Check if user exists by email
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user == null) {
                // Create new OAuth2 user
                String username = generateUsernameFromName(name, email);
                // Ensure username is unique
                int counter = 1;
                String baseUsername = username;
                while (userRepository.existsByUsername(username)) {
                    username = baseUsername + counter;
                    counter++;
                }
                
                // Generate a secure dummy password for OAuth users
                String dummyPassword = generateSecureDummyPassword();
                String encodedPassword = passwordEncoder.encode(dummyPassword);
                
                user = new User(username, email, encodedPassword, provider, providerId);
                
                // Assign default role
                Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                        .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                user.setRoles(Set.of(userRole));
                
                user = userRepository.save(user);
            } else {
                // Update existing user with OAuth2 provider info if not set
                if (user.getProvider() == null || user.getProvider().isEmpty()) {
                    user.setProvider(provider);
                    user.setProviderId(providerId);
                    // If password is null or empty, generate a dummy password
                    if (user.getPassword() == null || user.getPassword().isEmpty()) {
                        String dummyPassword = generateSecureDummyPassword();
                        user.setPassword(passwordEncoder.encode(dummyPassword));
                    }
                    user = userRepository.save(user);
                }
            }
            
            // Generate JWT token
            UserDetailsImpl userDetails = UserDetailsImpl.build(user);
            String jwt = jwtUtils.generateTokenFromUserDetails(userDetails);
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            
            return new JwtResponse(jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    roles);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String generateUsernameFromName(String name, String email) {
        String username;
        
        // If name is null or empty, fallback to email-based generation
        if (name == null || name.trim().isEmpty()) {
            // Extract username from email (part before @)
            username = email.split("@")[0];
            // Remove special characters and limit length
            username = username.replaceAll("[^a-zA-Z0-9]", "");
        } else {
            // Generate username from name
            // Convert to lowercase
            username = name.toLowerCase();
            // Replace spaces with hyphens
            username = username.replaceAll("\\s+", "-");
            // Remove special characters (keep only alphanumeric and hyphens)
            username = username.replaceAll("[^a-zA-Z0-9-]", "");
            // Remove consecutive hyphens
            username = username.replaceAll("-+", "-");
            // Remove leading/trailing hyphens
            username = username.replaceAll("^-+|-+$", "");
        }
        
        // Limit to 20 characters
        if (username.length() > 20) {
            username = username.substring(0, 20);
            // Remove trailing hyphen if truncated at hyphen
            username = username.replaceAll("-+$", "");
        }
        
        // Ensure username is not empty (fallback to "user" if empty)
        if (username.isEmpty()) {
            username = email != null && !email.isEmpty() ? email.split("@")[0] : "user";
            username = username.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (username.length() > 20) {
                username = username.substring(0, 20);
            }
        }
        
        return username;
    }

    private String generateSecureDummyPassword() {
        // Generate a secure random password for OAuth users
        // This password will never be used for authentication, but is required by the database
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}

