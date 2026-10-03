package com.ecommerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerLoginAndReadProfile() throws Exception {
        String email = uniqueEmail();

        doPost("/api/auth/register", null, registerBody("Asha Rai", email, PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        String token = login(email, PASSWORD);

        doGet("/api/users/profile", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.fullName").value("Asha Rai"));
    }

    @Test
    void passwordIsStoredAsBcryptHash() throws Exception {
        String email = uniqueEmail();
        doPost("/api/auth/register", null, registerBody("Asha Rai", email, PASSWORD))
                .andExpect(status().isCreated());

        String stored = userRepository.findByEmail(email).orElseThrow().getPassword();

        assertThat(stored).startsWith("$2").isNotEqualTo(PASSWORD);
    }

    @Test
    void registeringTheSameEmailTwice_isConflict_evenWithDifferentCase() throws Exception {
        String email = "Mixed-" + UUID.randomUUID() + "@Example.com";
        doPost("/api/auth/register", null, registerBody("Asha", email, PASSWORD))
                .andExpect(status().isCreated());

        doPost("/api/auth/register", null, registerBody("Asha", email.toLowerCase(), PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));

        login(email, PASSWORD); // logging in with the original capitalisation still works
    }

    @Test
    void invalidRegistration_returnsFieldErrors() throws Exception {
        doPost("/api/auth/register", null, registerBody("", "not-an-email", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.fullName").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    void wrongPasswordAndUnknownEmail_giveTheSameAnswer() throws Exception {
        String email = uniqueEmail();
        doPost("/api/auth/register", null, registerBody("Asha", email, PASSWORD))
                .andExpect(status().isCreated());

        doPost("/api/auth/login", null, "{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(email))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        doPost("/api/auth/login", null, "{\"email\":\"nobody-%s@example.com\",\"password\":\"x\"}"
                .formatted(UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void malformedJson_isBadRequest() throws Exception {
        doPost("/api/auth/login", null, "{bad json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"));
    }

    @Test
    void profileWithoutToken_isUnauthorized_withJsonError() throws Exception {
        doGet("/api/users/profile", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void profileWithGarbageToken_isUnauthorized() throws Exception {
        doGet("/api/users/profile", "not-a-real-token")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}