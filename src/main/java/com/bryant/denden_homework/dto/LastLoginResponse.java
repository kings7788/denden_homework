package com.bryant.denden_homework.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Response for GET /api/users/me/last-login. */
@Data
@AllArgsConstructor
public class LastLoginResponse {

    @Schema(example = "user@example.com")
    private String email;

    @JsonProperty("last_login_at")
    @Schema(name = "last_login_at", example = "2026-06-15T14:53:02.452582")
    private LocalDateTime lastLoginAt;
}
