package com.project.rentmebarwaha.auth.security;

import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userIdStr) throws UsernameNotFoundException {
        try {
            UUID userId = UUID.fromString(userIdStr);
            User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userIdStr));
            return UserPrincipal.create(user);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException("Invalid user id format: " + userIdStr);
        }
    }
}

