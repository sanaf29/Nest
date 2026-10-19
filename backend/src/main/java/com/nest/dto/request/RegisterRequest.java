package com.nest.dto.request;

import com.nest.validation.ValidCurrency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100)
        @Schema(example = "Sanae")
        String firstName,

        @NotBlank @Size(max = 100)
        @Schema(example = "Faghfouri")
        String lastName,

        @NotBlank @Email @Size(max = 254)
        @Schema(example = "sanae@example.com")
        String email,

        // 72 is BCrypt's input limit; longer passwords would be silently truncated.
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$", message = "must contain at least one letter and one digit")
        @Schema(example = "Str0ngPassw0rd")
        String password,

        @NotNull @ValidCurrency
        @Schema(example = "CAD")
        String preferredCurrency
) {
    @Override
    public String toString() {
        return "RegisterRequest[email=%s, password=***]".formatted(email);
    }
}
