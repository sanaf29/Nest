package com.nest.service;

import com.nest.dto.request.ChangePasswordRequest;
import com.nest.dto.request.UpdateProfileRequest;
import com.nest.dto.response.UserResponse;

/** Profile operations for the authenticated user. */
public interface UserService {

    UserResponse getProfile(long userId);

    UserResponse updateProfile(long userId, UpdateProfileRequest request);

    /**
     * Requires the current password. Existing tokens stay valid until they expire (stateless JWT;
     * see the security notes in the README).
     *
     * @throws com.nest.exception.InvalidCredentialsException if the current password is wrong
     */
    void changePassword(long userId, ChangePasswordRequest request);
}
