package com.saiteja.apigateway.security.services;

import com.saiteja.apigateway.model.User;
import com.saiteja.apigateway.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ReactiveUserDetailsServiceImpl implements ReactiveUserDetailsService {
    @Autowired
    UserRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return Mono.fromCallable(() -> {
                    User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));
                    return UserDetailsImpl.build(user);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .cast(UserDetails.class);
    }
}

