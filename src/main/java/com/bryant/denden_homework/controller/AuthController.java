package com.bryant.denden_homework.controller;

import com.bryant.denden_homework.dto.AccountResponse;
import com.bryant.denden_homework.dto.LoginRequest;
import com.bryant.denden_homework.dto.LoginResponse;
import com.bryant.denden_homework.dto.LoginVerifyRequest;
import com.bryant.denden_homework.dto.RegisterRequest;
import com.bryant.denden_homework.dto.TokenResponse;
import com.bryant.denden_homework.entity.User;
import com.bryant.denden_homework.security.JwtService;
import com.bryant.denden_homework.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public authentication endpoints: registration, activation and two-step login. */
@Tag(name = "Authentication", description = "註冊、開通、兩階段登入")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @Operation(summary = "註冊", description = "以 Email 為帳號建立會員(狀態 PENDING),並寄出開通信。")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public AccountResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.getEmail(), request.getPassword());
        return new AccountResponse(
                "Registration successful. Please check your email to activate your account.",
                user.getEmail(), user.getStatus().name());
    }

    @Operation(summary = "開通帳號", description = "用開通信中的 token 啟用帳號(狀態轉 ACTIVE)。")
    @GetMapping("/verify")
    public AccountResponse verify(@RequestParam String token) {
        User user = authService.verify(token);
        return new AccountResponse(
                "Account activated. You can now log in.",
                user.getEmail(), user.getStatus().name());
    }

    @Operation(summary = "登入(第一階段)",
            description = "驗證帳密(帳號須已開通),寄出 Email OTP,回傳 challenge_id。")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String challengeId = authService.login(request.getEmail(), request.getPassword());
        return new LoginResponse("A verification code has been sent to your email.", challengeId);
    }

    @Operation(summary = "登入(第二階段)",
            description = "用 challenge_id + Email 收到的 OTP 驗證,成功回傳 JWT access_token。")
    @PostMapping("/login/verify")
    public TokenResponse loginVerify(@Valid @RequestBody LoginVerifyRequest request) {
        User user = authService.verifyLogin(request.getChallengeId(), request.getOtp());
        String jwt = jwtService.generateToken(user.getEmail());
        return new TokenResponse("Bearer", jwt, user.getLastLoginAt());
    }
}
