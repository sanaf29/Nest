package com.nest.controller;

import com.nest.dto.request.UpdateProfileRequest;
import com.nest.dto.response.UserResponse;
import com.nest.security.CurrentUserProvider;
import com.nest.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Profile of the authenticated user. There is deliberately no {@code /api/users/{id}}: the
 * user is always taken from the token, so one user can never address another.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUser;

    public UserController(UserService userService, CurrentUserProvider currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current user's profile")
    public UserResponse me() {
        return userService.getProfile(currentUser.currentUserId());
    }

    @PutMapping("/me")
    @Operation(summary = "Update the current user's name and preferred currency")
    public UserResponse updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(currentUser.currentUserId(), request);
    }
}
