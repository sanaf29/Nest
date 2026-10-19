package com.nest.service;

import com.nest.dto.request.LoginRequest;
import com.nest.dto.request.RegisterRequest;
import com.nest.dto.response.AuthResponse;
import com.nest.dto.response.UserResponse;

/** Credential handling: account creation and login. */
public interface AuthService {

    /**
     * Creates a user with a hashed password.
     *
     * @throws com.nest.exception.ConflictException if the email is already registered
     */
    UserResponse register(RegisterRequest request);

    /**
     * Verifies credentials and issues an access token.
     *
     * @throws com.nest.exception.InvalidCredentialsException for an unknown email or a wrong password
     */
    AuthResponse login(LoginRequest request);
}
