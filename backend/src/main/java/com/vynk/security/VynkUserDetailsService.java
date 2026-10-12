package com.vynk.security;

import com.vynk.user.entity.AccountStatus;
import com.vynk.user.entity.User;
import com.vynk.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class VynkUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public VynkUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .or(() -> userRepository.findByEmailIgnoreCase(email))
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        boolean accountEnabled =
                user.getStatus() == AccountStatus.ACTIVE
                && user.isEmailVerified();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .disabled(!accountEnabled)
                .build();
    }
}