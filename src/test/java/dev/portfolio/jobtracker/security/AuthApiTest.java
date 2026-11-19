package dev.portfolio.jobtracker.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.portfolio.jobtracker.application.JobApplicationRepository;
import dev.portfolio.jobtracker.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/** Exercises the real sign-up / login / logout flow end to end through the security filters. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired JobApplicationRepository applications;

    @BeforeEach
    void clean() {
        applications.deleteAll();
        users.deleteAll();
    }

    private void register(String email, String password) throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","displayName":"Jamie"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                        .param("email", email).param("password", password))
                .andExpect(status().isNoContent())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    void registerLoginUseApiAndLogout() throws Exception {
        register("Jamie@Example.com", "correct-horse");

        // Email is case-insensitive.
        MockHttpSession session = login("jamie@example.com", "correct-horse");

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jamie@example.com"))
                .andExpect(jsonPath("$.displayName").value("Jamie"));

        mvc.perform(post("/api/applications").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company\":\"Acme\",\"role\":\"SWE\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void storesPasswordsAsBcryptHashes() throws Exception {
        register("jamie@example.com", "correct-horse");
        String hash = users.findByEmail("jamie@example.com").orElseThrow().getPasswordHash();
        assertThat(hash).startsWith("$2").doesNotContain("correct-horse");
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        register("jamie@example.com", "correct-horse");
        mvc.perform(post("/api/auth/login").with(csrf())
                        .param("email", "jamie@example.com").param("password", "wrong-password"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        register("jamie@example.com", "correct-horse");
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"JAMIE@example.com\",\"password\":\"another-pass\",\"displayName\":\"J\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void validatesRegistration() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\",\"displayName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.displayName").exists());
    }

    @Test
    void apiRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/applications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void stateChangingRequestsRequireCsrfToken() throws Exception {
        register("jamie@example.com", "correct-horse");
        MockHttpSession session = login("jamie@example.com", "correct-horse");

        mvc.perform(post("/api/applications").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company\":\"Acme\",\"role\":\"SWE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void staticFrontendIsPublic() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/app.js")).andExpect(status().isOk());
    }
}
