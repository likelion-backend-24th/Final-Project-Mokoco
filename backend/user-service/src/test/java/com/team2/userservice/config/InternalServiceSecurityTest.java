package com.team2.userservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(properties = "internal.service-key=test-internal-key")
@ContextConfiguration(classes = InternalServiceSecurityTest.ProbeController.class)
@Import(InternalServiceSecurityConfig.class)
class InternalServiceSecurityTest {
    @Autowired MockMvc mvc;

    @RestController
    static class ProbeController {
        @GetMapping("/internal/users/by-email")
        Map<String, Object> user() { return Map.of("id", 1, "email", "test@example.com"); }
    }

    @Test
    void missingKeyReturnsJson401WithoutLoginRedirect() throws Exception {
        mvc.perform(get("/internal/users/by-email"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_INTERNAL_SERVICE"));
    }

    @Test
    void incorrectKeyIsRejected() throws Exception {
        mvc.perform(get("/internal/users/by-email").header("X-Internal-Service-Key", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void correctKeyCanReadJsonAndDoesNotAuthenticateNextRequest() throws Exception {
        mvc.perform(get("/internal/users/by-email").header("X-Internal-Service-Key", "test-internal-key"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(get("/internal/users/by-email")).andExpect(status().isUnauthorized());
    }
}
