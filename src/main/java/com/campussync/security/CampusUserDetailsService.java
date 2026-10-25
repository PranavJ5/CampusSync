package com.campussync.security;

import com.campussync.entity.User;
import com.campussync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CampusUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .or(() -> userRepository.findByUsername(username))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        if (user.getRole() == null || ApprovedRole.fromExactCode(user.getRole().getCode()).isEmpty()) {
            throw new UsernameNotFoundException("User has no valid role");
        }
        return new CampusUserPrincipal(user.getId(), user.getUsername(), user.getEmail(),
                user.getPasswordHash(), user.isActive(), user.getRole().getCode());
    }
}
