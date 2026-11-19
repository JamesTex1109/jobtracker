package dev.portfolio.jobtracker.application;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.jayway.jsonpath.JsonPath;
import dev.portfolio.jobtracker.security.AuthUser;
import dev.portfolio.jobtracker.user.AppUser;
import dev.portfolio.jobtracker.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class JobApplicationApiTest {

    @Autowired MockMvc mvc;
    @Autowired JobApplicationRepository repository;
    @Autowired AppUserRepository users;

    RequestPostProcessor alice;
    RequestPostProcessor bob;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        users.deleteAll();
        alice = signedIn(users.save(new AppUser("alice@example.com", "{noop}unused", "Alice")));
        bob = signedIn(users.save(new AppUser("bob@example.com", "{noop}unused", "Bob")));
    }

    /** Acts as the given user, with a valid CSRF token, without going through the login form. */
    private static RequestPostProcessor signedIn(AppUser u) {
        RequestPostProcessor auth = user(AuthUser.from(u));
        return req -> csrf().postProcessRequest(auth.postProcessRequest(req));
    }

    private long create(String company, String role, String status) throws Exception {
        String body = """
                {"company":"%s","role":"%s","status":%s}
                """.formatted(company, role, status == null ? "null" : "\"" + status + "\"");
        String json = mvc.perform(post("/api/applications").with(alice).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void createsApplicationDefaultingToWishlist() throws Exception {
        mvc.perform(post("/api/applications").with(alice).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company":"Acme","role":"Backend Engineer","jobUrl":"https://acme.dev/jobs/1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("WISHLIST"))
                .andExpect(jsonPath("$.appliedDate").doesNotExist())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void rejectsInvalidInputWithFieldErrors() throws Exception {
        mvc.perform(post("/api/applications").with(alice).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company":"","role":"SWE","jobUrl":"ftp://nope"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.company").exists())
                .andExpect(jsonPath("$.errors.jobUrl").exists());
    }

    @Test
    void rejectsUnknownStatus() throws Exception {
        mvc.perform(post("/api/applications").with(alice).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company":"Acme","role":"SWE","status":"HIRED_LOL"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404ForMissingApplication() throws Exception {
        mvc.perform(get("/api/applications/999999").with(alice))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Job application 999999 not found"));
    }

    @Test
    void movingOutOfWishlistStampsAppliedDate() throws Exception {
        long id = create("Acme", "SWE", null);
        mvc.perform(patch("/api/applications/{id}/status", id).with(alice).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.appliedDate").exists());
    }

    @Test
    void updatesAndDeletes() throws Exception {
        long id = create("Acme", "SWE", "APPLIED");
        mvc.perform(put("/api/applications/{id}", id).with(alice).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company":"Acme Corp","role":"Senior SWE","status":"INTERVIEWING","notes":"Onsite Friday"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company").value("Acme Corp"))
                .andExpect(jsonPath("$.notes").value("Onsite Friday"));

        mvc.perform(delete("/api/applications/{id}", id).with(alice)).andExpect(status().isNoContent());
        mvc.perform(get("/api/applications/{id}", id).with(alice)).andExpect(status().isNotFound());
    }

    @Test
    void filtersByStatusAndSearchText() throws Exception {
        create("Google", "Software Engineer", "APPLIED");
        create("Stripe", "Backend Engineer", "INTERVIEWING");
        create("Netflix", "Data Engineer", "APPLIED");

        mvc.perform(get("/api/applications").with(alice).param("status", "APPLIED"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].company", not(hasItem("Stripe"))));

        mvc.perform(get("/api/applications").with(alice).param("q", "BACKEND"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].company").value("Stripe"));
    }

    @Test
    void usersCannotSeeOrModifyEachOthersApplications() throws Exception {
        long alicesId = create("Acme", "SWE", "APPLIED");

        mvc.perform(get("/api/applications/{id}", alicesId).with(bob)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/applications/{id}", alicesId).with(bob)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/applications/{id}/status", alicesId).with(bob)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OFFER\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/applications").with(bob)).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/applications/stats").with(bob)).andExpect(jsonPath("$.total").value(0));

        // Alice's data is untouched.
        mvc.perform(get("/api/applications/{id}", alicesId).with(alice))
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void computesStats() throws Exception {
        create("A", "SWE", "WISHLIST");
        create("B", "SWE", "APPLIED");
        create("C", "SWE", "APPLIED");
        create("D", "SWE", "INTERVIEWING");
        create("E", "SWE", "REJECTED");

        // 4 submitted, 2 responded -> 0.5
        mvc.perform(get("/api/applications/stats").with(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.byStatus.APPLIED").value(2))
                .andExpect(jsonPath("$.byStatus.OFFER").value(0))
                .andExpect(jsonPath("$.responseRate").value(0.5));
    }
}
