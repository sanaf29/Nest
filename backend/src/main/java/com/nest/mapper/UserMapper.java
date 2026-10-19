package com.nest.mapper;

import com.nest.dto.response.UserResponse;
import com.nest.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPreferredCurrency(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
