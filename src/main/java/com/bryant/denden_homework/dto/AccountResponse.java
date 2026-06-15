package com.bryant.denden_homework.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Response for register / verify. */
@Data
@AllArgsConstructor
public class AccountResponse {

    @Schema(example = "Registration successful. Please check your email to activate your account.")
    private String message;

    @Schema(example = "user@example.com")
    private String email;

    @Schema(example = "PENDING", allowableValues = {"PENDING", "ACTIVE"})
    private String status;
}
