package com.bryant.denden_homework.controller;

import com.bryant.denden_homework.dto.LastLoginResponse;
import com.bryant.denden_homework.entity.User;
import com.bryant.denden_homework.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated member endpoints. Requires a valid JWT (see SecurityConfig). */
@Tag(name = "Members", description = "需登入(JWT)的會員端點")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Returns the *caller's own* last login time. Identity comes solely from the
     * JWT (Authentication.getName()), so a user can never query another account.
     */
    @Operation(summary = "查詢自己的最後登入時間",
            description = "身分取自 JWT,只回傳呼叫者本人的資料,無法查詢他人。")
    @GetMapping("/me/last-login")
    public LastLoginResponse myLastLogin(Authentication authentication) {
        User user = userService.getByEmail(authentication.getName());
        return new LastLoginResponse(user.getEmail(), user.getLastLoginAt());
    }
}
