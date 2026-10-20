package com.nest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$", message = "must contain at least one letter and one digit")
        String newPassword
) {
    @Override
    public String toString() {
        return "ChangePasswordRequest[***]";
    }
}
