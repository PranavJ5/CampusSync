package com.campussync.controller;

import com.campussync.config.SecurityConfig;
import com.campussync.exception.ApiExceptionHandler;
import com.campussync.exception.AuthenticationExceptionHandler;
import com.campussync.security.JsonAccessDeniedHandler;
import com.campussync.security.JsonAuthenticationEntryPoint;
import com.campussync.security.JwtAuthenticationFilter;
import com.campussync.security.JwtService;
import com.campussync.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, AuthenticationExceptionHandler.class, ApiExceptionHandler.class,
        JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class, JwtAuthenticationFilter.class})
class DashboardControllerSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboards;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void studentDashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard/student"))
                .andExpect(status().isUnauthorized());
    }
}
