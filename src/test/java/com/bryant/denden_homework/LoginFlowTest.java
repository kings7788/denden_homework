package com.bryant.denden_homework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bryant.denden_homework.entity.TokenType;
import com.bryant.denden_homework.entity.User;
import com.bryant.denden_homework.entity.UserStatus;
import com.bryant.denden_homework.repository.UserRepository;
import com.bryant.denden_homework.repository.VerificationTokenRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LoginFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    /** Registers a user and forces it ACTIVE (activation flow covered elsewhere). */
    private void registerActiveUser(String email) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Test
    void twoStageLoginThenQueryOwnLastLogin() throws Exception {
        registerActiveUser("dave@example.com");

        // Stage 1: email + password -> challengeId, OTP emailed
        String loginResp = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dave@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challenge_id").exists())
                .andReturn().getResponse().getContentAsString();
        String challengeId = JsonPath.read(loginResp, "$.challenge_id");

        // Read the OTP that was generated (in tests email is only logged)
        String otp = tokenRepository
                .findFirstByUser_EmailAndType("dave@example.com", TokenType.LOGIN_OTP)
                .orElseThrow().getCode();

        // Stage 2: challengeId + OTP -> JWT
        String verifyResp = mvc.perform(post("/api/auth/login/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challenge_id\":\"" + challengeId + "\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        String jwt = JsonPath.read(verifyResp, "$.access_token");

        // Query own last-login with the JWT
        mvc.perform(get("/api/users/me/last-login").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("dave@example.com"))
                .andExpect(jsonPath("$.last_login_at").exists());
    }

    @Test
    void lastLoginRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/users/me/last-login"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJwtIsUnauthorized() throws Exception {
        mvc.perform(get("/api/users/me/last-login").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordRejected() throws Exception {
        registerActiveUser("erin@example.com");
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"erin@example.com\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void inactiveAccountCannotLogin() throws Exception {
        // Register but DO NOT activate
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"frank@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"frank@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongOtpRejected() throws Exception {
        registerActiveUser("grace@example.com");
        String loginResp = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"grace@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String challengeId = JsonPath.read(loginResp, "$.challenge_id");

        mvc.perform(post("/api/auth/login/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeId\":\"" + challengeId + "\",\"otp\":\"000000\"}"))
                .andExpect(status().isBadRequest());
    }
}
