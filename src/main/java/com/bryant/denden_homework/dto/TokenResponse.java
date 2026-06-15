package com.bryant.denden_homework.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Response for login step 2 (issues the JWT). */
@Data
@AllArgsConstructor
public class TokenResponse {

    @JsonProperty("token_type")
    @Schema(name = "token_type", example = "Bearer")
    private String tokenType;

    @JsonProperty("access_token")
    @Schema(name = "access_token", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0...")
    private String accessToken;

    @JsonProperty("last_login_at")
    @Schema(name = "last_login_at", example = "2026-06-15T14:53:02.452582")
    private LocalDateTime lastLoginAt;
}
