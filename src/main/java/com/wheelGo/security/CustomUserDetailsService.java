package com.wheelGo.security;

import com.wheelGo.model.user.User;
import com.wheelGo.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        String tenantSlug = user.getTenant() != null ? user.getTenant().getSlug() : null;
        var tenantId = user.getTenant() != null ? user.getTenant().getId() : null;

        return new CustomUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole().name(),
                tenantId,
                tenantSlug,
                false,
                null,
                null
        );
    }
}