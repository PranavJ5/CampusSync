package com.campussync.security;

import com.campussync.entity.Role;
import com.campussync.entity.User;
import com.campussync.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CampusUserDetailsServiceSecurityTest {

    @Test
    void mapsRoleToSpringRoleAuthority() {
        UserRepository repository = mock(UserRepository.class);
        User user = user("FACULTY", true);
        when(repository.findByEmail("faculty@example.com")).thenReturn(Optional.of(user));

        CampusUserPrincipal principal = (CampusUserPrincipal) new CampusUserDetailsService(repository)
                .loadUserByUsername("faculty@example.com");

        assertEquals("ROLE_FACULTY", principal.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void preservesInactiveAccountState() {
        UserRepository repository = mock(UserRepository.class);
        User user = user("STUDENT", false);
        when(repository.findByEmail("student@example.com")).thenReturn(Optional.of(user));

        CampusUserPrincipal principal = (CampusUserPrincipal) new CampusUserDetailsService(repository)
                .loadUserByUsername("student@example.com");

        assertFalse(principal.isEnabled());
    }

    @Test
    void rejectsUserWithoutRole() {
        UserRepository repository = mock(UserRepository.class);
        User user = user(null, true);
        when(repository.findByEmail("norole@example.com")).thenReturn(Optional.of(user));

        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> new CampusUserDetailsService(repository).loadUserByUsername("norole@example.com"));
    }

    private User user(String roleCode, boolean active) {
        User user = new User();
        user.setId(1L);
        user.setUsername("test-user");
        user.setEmail(roleCode == null ? "norole@example.com" : roleCode.toLowerCase() + "@example.com");
        user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("password"));
        user.setActive(active);
        if (roleCode != null) {
            Role role = new Role();
            role.setCode(roleCode);
            user.setRole(role);
        }
        return user;
    }
}
