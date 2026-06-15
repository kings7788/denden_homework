package com.bryant.denden_homework.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for POST /api/auth/login (first factor: email + password). */
@Data
public class LoginRequest {

    @Schema(example = "user@example.com")
    @NotBlank
    @Email
    private String email;

    @Schema(example = "password123")
    @NotBlank
    private String password;
}
