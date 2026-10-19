package com.nest.service.impl;

import com.nest.dto.request.LoginRequest;
import com.nest.dto.request.RegisterRequest;
import com.nest.dto.response.AuthResponse;
import com.nest.dto.response.UserResponse;
import com.nest.exception.ConflictException;
import com.nest.exception.InvalidCredentialsException;
import com.nest.mapper.UserMapper;
import com.nest.model.User;
import com.nest.repository.UserRepository;
import com.nest.security.AccessToken;
import com.nest.security.TokenService;
import com.nest.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserMapper userMapper;

    /**
     * A real hash checked when the email is unknown, so "no such user" takes about as long as
     * "wrong password". Without it, response timing would reveal which emails are registered.
     */
    private final String timingEqualizerHash;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           TokenService tokenService,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.userMapper = userMapper;
        this.timingEqualizerHash = passwordEncoder.encode("nest-timing-equalizer");
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists.");
        }
        // A concurrent registration that slips past this check is stopped by the UQ_users_email
        // constraint, which GlobalExceptionHandler reports as 409.
        User user = new User(
                request.firstName(),
                request.lastName(),
                email,
                passwordEncoder.encode(request.password()),
                request.preferredCurrency());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Optional<User> candidate = userRepository.findByEmail(User.normalizeEmail(request.email()));
        if (candidate.isEmpty()) {
            passwordEncoder.matches(request.password(), timingEqualizerHash);
            throw new InvalidCredentialsException();
        }
        User user = candidate.get();
        if (!user.passwordMatches(request.password(), passwordEncoder)) {
            throw new InvalidCredentialsException();
        }
        AccessToken token = tokenService.issue(user);
        return AuthResponse.bearer(token.value(), token.expiresAt(), userMapper.toResponse(user));
    }
}
