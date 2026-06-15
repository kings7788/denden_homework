package com.bryant.denden_homework.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for POST /api/auth/login/verify (second factor: emailed OTP). */
@Data
public class LoginVerifyRequest {

    @JsonProperty("challenge_id")
    @Schema(name = "challenge_id", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank
    private String challengeId;

    @Schema(example = "123456")
    @NotBlank
    private String otp;
}
