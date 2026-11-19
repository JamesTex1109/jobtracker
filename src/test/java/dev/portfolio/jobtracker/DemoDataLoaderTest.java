package dev.portfolio.jobtracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.portfolio.jobtracker.application.ApplicationStatus;
import dev.portfolio.jobtracker.application.JobApplication;
import dev.portfolio.jobtracker.application.JobApplicationRepository;
import dev.portfolio.jobtracker.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DemoDataLoaderTest {

    @Autowired DemoDataLoader loader;
    @Autowired AppUserRepository users;
    @Autowired JobApplicationRepository applications;
    @Autowired MockMvc mvc;

    @Test
    void resetRestoresSampleDataAndRemovesVisitorEdits() {
        long demoId = users.findByEmail(DemoDataLoader.DEMO_EMAIL).orElseThrow().getId();
        applications.save(new JobApplication(demoId, "Graffiti Inc", "lol", ApplicationStatus.OFFER));

        loader.reset();

        var companies = applications.findAll().stream()
                .filter(a -> a.getOwnerId() == demoId)
                .map(JobApplication::getCompany)
                .toList();
        assertThat(companies).hasSize(8).doesNotContain("Graffiti Inc");
        assertThat(users.findAll()).filteredOn(u -> u.getEmail().equals(DemoDataLoader.DEMO_EMAIL)).hasSize(1);
    }

    @Test
    void exposesDemoLoginPublicly() throws Exception {
        mvc.perform(get("/api/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(DemoDataLoader.DEMO_EMAIL));
    }
}
