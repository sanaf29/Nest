package com.nest.service;

import com.nest.dto.request.UpdateProfileRequest;
import com.nest.dto.response.UserResponse;

/** Profile operations for the authenticated user. */
public interface UserService {

    UserResponse getProfile(long userId);

    UserResponse updateProfile(long userId, UpdateProfileRequest request);
}
