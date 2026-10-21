package com.campussync.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterSecurityTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesValidBearerTokenUsingCurrentDatabaseUser() throws Exception {
        JwtService jwtService = new JwtService(SECRET, 3600);
        CampusUserPrincipal principal = principal(true);
        var users = mock(org.springframework.security.core.userdetails.UserDetailsService.class);
        when(users.loadUserByUsername(principal.getEmail())).thenReturn(principal);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, users);

        invoke(filter, "Bearer " + jwtService.generateToken(principal));

        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
        assertEqualsPrincipal(principal);
    }

    @Test
    void leavesMissingMalformedExpiredAndInvalidSignatureTokensUnauthenticated() throws Exception {
        JwtService service = new JwtService(SECRET, 3600);
        var users = mock(org.springframework.security.core.userdetails.UserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(service, users);

        invoke(filter, null);
        assertUnauthenticated();
        invoke(filter, "Bearer not-a-jwt");
        assertUnauthenticated();
        invoke(filter, "Bearer " + expiredToken());
        assertUnauthenticated();
        String otherSignature = new JwtService("another-test-secret-that-is-at-least-32-bytes", 3600)
                .generateToken(principal(true));
        invoke(filter, "Bearer " + otherSignature);
        assertUnauthenticated();
    }

    @Test
    void leavesUnknownAndInactiveUsersUnauthenticated() throws Exception {
        JwtService service = new JwtService(SECRET, 3600);
        var users = mock(org.springframework.security.core.userdetails.UserDetailsService.class);
        CampusUserPrincipal active = principal(true);
        when(users.loadUserByUsername(active.getEmail())).thenThrow(new UsernameNotFoundException("not found"));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(service, users);

        invoke(filter, "Bearer " + service.generateToken(active));
        assertUnauthenticated();

        CampusUserPrincipal inactive = principal(false);
        when(users.loadUserByUsername(inactive.getEmail())).thenReturn(inactive);
        invoke(filter, "Bearer " + service.generateToken(inactive));
        assertUnauthenticated();
    }

    private void invoke(JwtAuthenticationFilter filter, String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        FilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
    }

    private String expiredToken() {
        return Jwts.builder()
                .subject("student@example.com")
                .expiration(new Date(0))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private CampusUserPrincipal principal(boolean active) {
        String email = active ? "student@example.com" : "inactive@example.com";
        return new CampusUserPrincipal(7L, "student", email, "hash", active, "STUDENT");
    }

    private void assertUnauthenticated() {
        assertFalse(SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
    }

    private void assertEqualsPrincipal(CampusUserPrincipal expected) {
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getPrincipal() == expected);
    }
}
