package dev.portfolio.jobtracker.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class JobApplicationTest {

    @Test
    void wishlistDoesNotSetAppliedDate() {
        var app = new JobApplication("Acme", "SWE", ApplicationStatus.WISHLIST);
        app.changeStatus(ApplicationStatus.WISHLIST);
        assertThat(app.getAppliedDate()).isNull();
    }

    @Test
    void keepsExistingAppliedDate() {
        var app = new JobApplication("Acme", "SWE", ApplicationStatus.WISHLIST);
        LocalDate earlier = LocalDate.of(2026, 1, 15);
        app.setAppliedDate(earlier);
        app.changeStatus(ApplicationStatus.INTERVIEWING);
        assertThat(app.getAppliedDate()).isEqualTo(earlier);
    }
}
