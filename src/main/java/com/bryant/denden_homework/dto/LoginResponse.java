package com.bryant.denden_homework.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Response for login step 1 (OTP sent, returns the challenge id). */
@Data
@AllArgsConstructor
public class LoginResponse {

    @Schema(example = "A verification code has been sent to your email.")
    private String message;

    @JsonProperty("challenge_id")
    @Schema(name = "challenge_id", example = "550e8400-e29b-41d4-a716-446655440000")
    private String challengeId;
}
