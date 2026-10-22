package com.campussync.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationProviderSecurityTest {

    @Test
    void verifiesValidBcryptPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        CampusUserPrincipal principal = principal(true, encoder.encode("correct-password"));
        UserDetailsService users = mock(UserDetailsService.class);
        when(users.loadUserByUsername(principal.getEmail())).thenReturn(principal);
        DaoAuthenticationProvider provider = provider(users, encoder);

        var authentication = provider.authenticate(
                new UsernamePasswordAuthenticationToken(principal.getEmail(), "correct-password"));

        assertTrue(authentication.isAuthenticated());
    }

    @Test
    void rejectsIncorrectPasswordAndInactiveAccount() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        UserDetailsService users = mock(UserDetailsService.class);
        CampusUserPrincipal active = principal(true, encoder.encode("correct-password"));
        when(users.loadUserByUsername(active.getEmail())).thenReturn(active);
        DaoAuthenticationProvider provider = provider(users, encoder);

        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
                new UsernamePasswordAuthenticationToken(active.getEmail(), "wrong-password")));

        CampusUserPrincipal inactive = principal(false, encoder.encode("correct-password"));
        when(users.loadUserByUsername(inactive.getEmail())).thenReturn(inactive);
        assertThrows(DisabledException.class, () -> provider.authenticate(
                new UsernamePasswordAuthenticationToken(inactive.getEmail(), "correct-password")));
    }

    private DaoAuthenticationProvider provider(UserDetailsService users, BCryptPasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    private CampusUserPrincipal principal(boolean active, String hash) {
        return new CampusUserPrincipal(7L, "student", "student@example.com", hash, active, "STUDENT");
    }
}
