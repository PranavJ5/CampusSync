package com.campussync.controller;

import com.campussync.dto.CurrentUserResponse;
import com.campussync.dto.LoginRequest;
import com.campussync.dto.LoginResponse;
import com.campussync.security.CampusUserPrincipal;
import com.campussync.security.JwtService;
import com.campussync.service.StudentAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final StudentAccountService studentAccounts;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        CampusUserPrincipal principal = (CampusUserPrincipal) authentication.getPrincipal();
        if (request.selectedRole() != null && !request.selectedRole().equals(principal.getRoleCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This account does not match the selected sign-in option.");
        }
        CurrentUserResponse user = new CurrentUserResponse(principal.getUserId(), principal.getUsername(),
                principal.getEmail(), principal.getRoleCode());
        return new LoginResponse(jwtService.generateToken(principal), "Bearer",
                jwtService.getExpirationSeconds(), user);
    }

    @PostMapping("/register")
    public ResponseEntity<CurrentUserResponse> registerStudent(
            @Valid @RequestBody com.campussync.dto.StudentAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentAccounts.register(request));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> currentUser(@AuthenticationPrincipal CampusUserPrincipal principal) {
        return ResponseEntity.ok(new CurrentUserResponse(principal.getUserId(), principal.getUsername(),
                principal.getEmail(), principal.getRoleCode()));
    }
}
