package com.example.studio.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.example.studio.IntegrationTestBase;

import jakarta.servlet.http.Cookie;

class AuthApiTest extends IntegrationTestBase {

    @Test
    void registerLogsInAndLogoutInvalidatesSession() throws Exception {
        Cookie session = register("Flow@Example.com");
        assertThat(session).isNotNull();
        assertThat(session.isHttpOnly()).isTrue();

        mvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flow@example.com"))
                .andExpect(jsonPath("$.displayName").value("tester"));

        mvc.perform(post("/api/auth/logout").cookie(session))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        register("dup@example.com");

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", "DUP@example.com", "password", "Passw0rd!"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_EXISTS"));
    }

    @Test
    void rejectsInvalidRegistration() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", "not-an-email", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", "demo1@example.com", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void seededDemoAccountCanLogIn() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", "demo1@example.com", "password", "Demo@123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("演示用户一"))
                .andReturn();
        Cookie session = result.getResponse().getCookie("SESSION");

        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isOk());
    }

    @Test
    void rejectsUnknownSession() throws Exception {
        mvc.perform(get("/api/auth/me").cookie(new Cookie("SESSION", "forged-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
