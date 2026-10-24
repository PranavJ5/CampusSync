package com.campussync.controller;

import com.campussync.dto.CurrentUserResponse;
import com.campussync.dto.LoginRequest;
import com.campussync.dto.LoginResponse;
import com.campussync.security.CampusUserPrincipal;
import com.campussync.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        CampusUserPrincipal principal = (CampusUserPrincipal) authentication.getPrincipal();
        return new LoginResponse(jwtService.generateToken(principal), "Bearer", jwtService.getExpirationSeconds());
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> currentUser(@AuthenticationPrincipal CampusUserPrincipal principal) {
        return ResponseEntity.ok(new CurrentUserResponse(principal.getUserId(), principal.getUsername(),
                principal.getEmail(), principal.getRoleCode()));
    }
}
