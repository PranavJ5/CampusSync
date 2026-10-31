package com.campussync.controller;

import com.campussync.exception.AuthenticationExceptionHandler;
import com.campussync.security.CampusUserPrincipal;
import com.campussync.security.JsonAccessDeniedHandler;
import com.campussync.security.JsonAuthenticationEntryPoint;
import com.campussync.security.JwtAuthenticationFilter;
import com.campussync.security.JwtService;
import com.campussync.config.SecurityConfig;
import com.campussync.exception.ApiExceptionHandler;
import com.campussync.service.StudentAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthenticationExceptionHandler.class, ApiExceptionHandler.class,
        JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class, JwtAuthenticationFilter.class})
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private StudentAccountService studentAccounts;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    void successfulLoginReturnsTokenResponse() throws Exception {
        CampusUserPrincipal principal = administratorPrincipal();
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(jwtService.generateToken(principal)).thenReturn("test-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@campussync.edu\",\"password\":\"correct-password\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.user.userId").value(1))
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.user.email").value("admin@campussync.edu"))
                .andExpect(jsonPath("$.user.role").value("ADMINISTRATOR"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void successfulStudentLoginPreservesStudentRole() throws Exception {
        CampusUserPrincipal principal = principal();
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(jwtService.generateToken(principal)).thenReturn("test-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.com\",\"password\":\"correct-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("student@example.com"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"));
    }

    @Test
    void successfulFacultyLoginUsesAuthenticatedFacultyRole() throws Exception {
        CampusUserPrincipal principal = new CampusUserPrincipal(
                12L, "faculty", "faculty@example.edu", "hash", true, "FACULTY");
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(jwtService.generateToken(principal)).thenReturn("faculty-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"faculty@example.edu\",\"password\":\"correct-password\","
                                + "\"selectedRole\":\"FACULTY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("FACULTY"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void rejectsLoginWhenAuthenticatedRoleDoesNotMatchSelectedOption() throws Exception {
        CampusUserPrincipal principal = principal();
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.com\",\"password\":\"correct-password\","
                                + "\"selectedRole\":\"ADMINISTRATOR\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("This account does not match the selected sign-in option."));

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void publicRegistrationIgnoresSubmittedRoleAndReturnsStudentRole() throws Exception {
        when(studentAccounts.register(any())).thenReturn(
                new com.campussync.dto.CurrentUserResponse(8L, "newstudent", "new@example.com", "STUDENT"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newstudent\",\"email\":\"new@example.com\","
                                + "\"password\":\"secure-password\",\"firstName\":\"New\",\"lastName\":\"Student\","
                                + "\"enrollmentNumber\":\"E100\",\"departmentId\":2,\"currentSemester\":1,"
                                + "\"admissionYear\":2026,\"role\":\"ADMINISTRATOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void missingStudentRoleIsReportedAsActionableServiceUnavailable() throws Exception {
        when(studentAccounts.register(any())).thenThrow(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Student account registration is temporarily unavailable because the STUDENT role is not configured. "
                        + "Contact a CampusSync administrator."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newstudent\",\"email\":\"new@example.com\","
                                + "\"password\":\"secure-password\",\"firstName\":\"New\",\"lastName\":\"Student\","
                                + "\"enrollmentNumber\":\"E100\",\"departmentId\":2,\"currentSemester\":1,"
                                + "\"admissionYear\":2026}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(
                        "Student account registration is temporarily unavailable because the STUDENT role is not configured. "
                                + "Contact a CampusSync administrator."));
    }

    @Test
    void invalidLoginReturnsJson401() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("invalid_credentials"));
    }

    @Test
    void currentUserRequiresAuthenticationAndReturnsPublicFields() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("authentication_required"));

        mockMvc.perform(get("/api/auth/me")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal(), null, principal().getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.email").value("student@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    private CampusUserPrincipal principal() {
        return new CampusUserPrincipal(7L, "student", "student@example.com", "hash", true, "STUDENT");
    }

    private CampusUserPrincipal administratorPrincipal() {
        return new CampusUserPrincipal(1L, "admin", "admin@campussync.edu", "hash", true, "ADMINISTRATOR");
    }
}
