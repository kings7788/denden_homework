package com.bryant.denden_homework.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request body for POST /api/auth/register. */
@Data
public class RegisterRequest {

    @Schema(example = "user@example.com")
    @NotBlank
    @Email(message = "must be a valid email address")
    private String email;

    @Schema(example = "password123")
    @NotBlank
    @Size(min = 8, message = "password must be at least 8 characters")
    private String password;
}
