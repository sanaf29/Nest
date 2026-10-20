package com.nest.service.impl;

import com.nest.dto.request.ChangePasswordRequest;
import com.nest.dto.request.UpdateProfileRequest;
import com.nest.exception.BusinessRuleException;
import com.nest.exception.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.nest.dto.response.UserResponse;
import com.nest.exception.ResourceNotFoundException;
import com.nest.mapper.UserMapper;
import com.nest.model.User;
import com.nest.repository.UserRepository;
import com.nest.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(long userId) {
        return userMapper.toResponse(findUser(userId));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.updateProfile(request.firstName(), request.lastName(), request.preferredCurrency());
        // Flush so @PreUpdate sets updatedAt before the response is built.
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public void changePassword(long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!user.passwordMatches(request.currentPassword(), passwordEncoder)) {
            throw new InvalidCredentialsException();
        }
        if (user.passwordMatches(request.newPassword(), passwordEncoder)) {
            throw new BusinessRuleException("Choose a password different from your current one.");
        }
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(user);
    }

    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
