package com.nest.controller;

import com.nest.dto.request.LoginRequest;
import com.nest.dto.request.RegisterRequest;
import com.nest.dto.response.AuthResponse;
import com.nest.dto.response.UserResponse;
import com.nest.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
@SecurityRequirements // public endpoints: no bearer token
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user",
            description = "201 on success; 400 invalid input; 409 email already registered.")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse created = authService.register(request);
        return ResponseEntity.created(URI.create("/api/users/me")).body(created);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive a bearer token",
            description = "200 with token; 400 invalid input; 401 invalid email or password.")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
