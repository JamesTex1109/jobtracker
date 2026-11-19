package dev.portfolio.jobtracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.portfolio.jobtracker.application.JobApplicationRepository;
import dev.portfolio.jobtracker.security.AuthUser;
import dev.portfolio.jobtracker.user.AppUser;
import dev.portfolio.jobtracker.user.AppUserRepository;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Runs the app against a real PostgreSQL in Docker, to catch anything H2's
 * PostgreSQL mode doesn't faithfully emulate (migrations, constraints, query typing).
 * Skipped automatically on machines without Docker; always runs in CI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired MockMvc mvc;
    @Autowired DataSource dataSource;
    @Autowired AppUserRepository users;
    @Autowired JobApplicationRepository applications;

    AppUser owner;
    RequestPostProcessor asOwner;

    @BeforeEach
    void setUp() {
        applications.deleteAll();
        users.deleteAll();
        owner = users.save(new AppUser("pg@example.com", "{noop}unused", "Pg"));
        RequestPostProcessor auth = user(AuthUser.from(owner));
        asOwner = req -> csrf().postProcessRequest(auth.postProcessRequest(req));
    }

    private void create(String company, String role, String status) throws Exception {
        mvc.perform(post("/api/applications").with(asOwner).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company":"%s","role":"%s","status":"%s"}
                                """.formatted(company, role, status)))
                .andExpect(status().isCreated());
    }

    @Test
    void runsOnRealPostgres() throws Exception {
        try (var conn = dataSource.getConnection()) {
            assertThat(conn.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }
    }

    @Test
    void searchWorksWithAndWithoutOptionalFilters() throws Exception {
        create("Google", "Software Engineer", "APPLIED");
        create("Stripe", "Backend Engineer", "INTERVIEWING");

        // Optional filters absent: the classic place where `:param IS NULL` JPQL breaks on PostgreSQL.
        mvc.perform(get("/api/applications").with(asOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/applications").with(asOwner).param("status", "INTERVIEWING").param("q", "stripe"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/applications/stats").with(asOwner))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.responseRate").value(0.5));
    }

    @Test
    void emailUniquenessIsEnforcedByTheDatabase() {
        assertThatThrownBy(() -> users.saveAndFlush(new AppUser("pg@example.com", "{noop}x", "Dup")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingAUserCascadesToTheirApplications() throws Exception {
        create("Acme", "SWE", "APPLIED");
        try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM users WHERE id = " + owner.getId());
        }
        assertThat(applications.count()).isZero();
    }
}
