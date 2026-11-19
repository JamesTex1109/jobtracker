package dev.portfolio.jobtracker;

import dev.portfolio.jobtracker.application.ApplicationStatus;
import dev.portfolio.jobtracker.application.JobApplication;
import dev.portfolio.jobtracker.application.JobApplicationRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Seeds sample data when run with --spring.profiles.active=demo. */
@Component
@Profile("demo")
class DemoDataLoader implements CommandLineRunner {

    private final JobApplicationRepository repository;

    DemoDataLoader(JobApplicationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        repository.saveAll(List.of(
                app("Northwind Labs", "Software Engineer I", ApplicationStatus.WISHLIST, "Remote", null),
                app("Blue Harbor Bank", "Backend Developer", ApplicationStatus.WISHLIST, "Chicago, IL", null),
                app("Cascade Health", "Java Developer", ApplicationStatus.APPLIED, "Austin, TX", today.minusDays(3)),
                app("Orbit Logistics", "Software Engineer", ApplicationStatus.APPLIED, "Dallas, TX", today.minusDays(9)),
                app("Pinecrest Games", "Tools Engineer (C++)", ApplicationStatus.APPLIED, "Seattle, WA", today.minusDays(12)),
                app("Lumen Analytics", "Platform Engineer", ApplicationStatus.INTERVIEWING, "Remote", today.minusDays(16)),
                app("Redwood Systems", "Associate SWE", ApplicationStatus.OFFER, "Denver, CO", today.minusDays(30)),
                app("Atlas Retail", "Full-Stack Engineer", ApplicationStatus.REJECTED, "Atlanta, GA", today.minusDays(21))));
    }

    private static JobApplication app(String company, String role, ApplicationStatus status,
                                      String location, LocalDate applied) {
        JobApplication a = new JobApplication(company, role, status);
        a.setLocation(location);
        a.setAppliedDate(applied);
        return a;
    }
}
