package com.bryant.denden_homework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bryant.denden_homework.entity.TokenType;
import com.bryant.denden_homework.entity.UserStatus;
import com.bryant.denden_homework.entity.VerificationToken;
import com.bryant.denden_homework.repository.UserRepository;
import com.bryant.denden_homework.repository.VerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRegistrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Test
    void registerThenVerifyActivatesAccount() throws Exception {
        // 1) Register -> 201, account PENDING, password stored hashed
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        var alice = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(alice.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(alice.getPassword()).isNotEqualTo("password123"); // BCrypt-hashed

        VerificationToken activation = tokenRepository
                .findFirstByUser_EmailAndType("alice@example.com", TokenType.ACTIVATION)
                .orElseThrow();

        // 2) Verify with the activation token -> 200, account ACTIVE
        mvc.perform(get("/api/auth/verify").param("token", activation.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(userRepository.findByEmail("alice@example.com").orElseThrow().getStatus())
                .isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String body = "{\"email\":\"bob@example.com\",\"password\":\"password123\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already registered"));
    }

    @Test
    void invalidEmailIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void shortPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"carol@example.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void invalidActivationTokenIsRejected() throws Exception {
        mvc.perform(get("/api/auth/verify").param("token", "does-not-exist"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid activation token"));
    }
}
