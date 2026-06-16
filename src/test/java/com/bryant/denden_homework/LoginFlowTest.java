package com.bryant.denden_homework;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bryant.denden_homework.entity.User;
import com.bryant.denden_homework.entity.UserStatus;
import com.bryant.denden_homework.repository.UserRepository;
import com.bryant.denden_homework.service.OtpGenerator;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LoginFlowTest {

    private static final String OTP = "123456";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    /** Fixed OTP so the test knows the code without reading the (now hashed) DB value. */
    @MockitoBean
    private OtpGenerator otpGenerator;

    @BeforeEach
    void stubOtp() {
        when(otpGenerator.generate()).thenReturn(OTP);
    }

    private void registerActiveUser(String email) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    private String startLogin(String email) throws Exception {
        String resp = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challenge_id").exists())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resp, "$.challenge_id");
    }

    private String loginAndGetJwt(String email) throws Exception {
        String challengeId = startLogin(email);
        String resp = mvc.perform(post("/api/auth/login/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challenge_id\":\"" + challengeId + "\",\"otp\":\"" + OTP + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resp, "$.access_token");
    }

    @Test
    void twoStageLoginThenQueryOwnLastLogin() throws Exception {
        registerActiveUser("dave@example.com");
        String jwt = loginAndGetJwt("dave@example.com");

        mvc.perform(get("/api/users/me/last-login").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("dave@example.com"))
                .andExpect(jsonPath("$.last_login_at").exists());
    }

    @Test
    void eachUserSeesOnlyOwnData() throws Exception {
        registerActiveUser("amy@example.com");
        registerActiveUser("ben@example.com");
        String amyJwt = loginAndGetJwt("amy@example.com");
        String benJwt = loginAndGetJwt("ben@example.com");

        mvc.perform(get("/api/users/me/last-login").header("Authorization", "Bearer " + amyJwt))
                .andExpect(jsonPath("$.email").value("amy@example.com"));
        mvc.perform(get("/api/users/me/last-login").header("Authorization", "Bearer " + benJwt))
                .andExpect(jsonPath("$.email").value("ben@example.com"));
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
        String challengeId = startLogin("grace@example.com");

        mvc.perform(post("/api/auth/login/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challenge_id\":\"" + challengeId + "\",\"otp\":\"000000\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void challengeBurnedAfterMaxAttempts() throws Exception {
        registerActiveUser("heidi@example.com");
        String challengeId = startLogin("heidi@example.com");

        // 5 wrong attempts -> challenge burned
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"challenge_id\":\"" + challengeId + "\",\"otp\":\"000000\"}"))
                    .andExpect(status().isBadRequest());
        }
        // even the CORRECT otp now fails — the challenge is used up
        mvc.perform(post("/api/auth/login/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challenge_id\":\"" + challengeId + "\",\"otp\":\"" + OTP + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void previousOtpInvalidatedOnNewLogin() throws Exception {
        registerActiveUser("ivan@example.com");
        String firstChallenge = startLogin("ivan@example.com");
        String secondChallenge = startLogin("ivan@example.com"); // invalidates the first

        // first challenge no longer usable
        mvc.perform(post("/api/auth/login/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challenge_id\":\"" + firstChallenge + "\",\"otp\":\"" + OTP + "\"}"))
                .andExpect(status().isConflict());
        // second challenge works
        mvc.perform(post("/api/auth/login/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challenge_id\":\"" + secondChallenge + "\",\"otp\":\"" + OTP + "\"}"))
                .andExpect(status().isOk());
    }
}
