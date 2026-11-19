package dev.portfolio.jobtracker;

import dev.portfolio.jobtracker.application.ApplicationStatus;
import dev.portfolio.jobtracker.application.JobApplication;
import dev.portfolio.jobtracker.application.JobApplicationRepository;
import dev.portfolio.jobtracker.user.AppUser;
import dev.portfolio.jobtracker.user.AppUserRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Seeds a demo account with sample data when run with --spring.profiles.active=demo. */
@Component
@Profile("demo")
class DemoDataLoader implements CommandLineRunner {

    static final String DEMO_EMAIL = "demo@jobtracker.dev";
    static final String DEMO_PASSWORD = "demo1234";

    private final AppUserRepository users;
    private final JobApplicationRepository applications;
    private final PasswordEncoder passwordEncoder;

    DemoDataLoader(AppUserRepository users, JobApplicationRepository applications, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.applications = applications;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (users.existsByEmail(DEMO_EMAIL)) {
            return;
        }
        long owner = users.save(new AppUser(DEMO_EMAIL, passwordEncoder.encode(DEMO_PASSWORD), "Demo User")).getId();
        LocalDate today = LocalDate.now();
        applications.saveAll(List.of(
                app(owner, "Northwind Labs", "Software Engineer I", ApplicationStatus.WISHLIST, "Remote", null),
                app(owner, "Blue Harbor Bank", "Backend Developer", ApplicationStatus.WISHLIST, "Chicago, IL", null),
                app(owner, "Cascade Health", "Java Developer", ApplicationStatus.APPLIED, "Austin, TX", today.minusDays(3)),
                app(owner, "Orbit Logistics", "Software Engineer", ApplicationStatus.APPLIED, "Dallas, TX", today.minusDays(9)),
                app(owner, "Pinecrest Games", "Tools Engineer (C++)", ApplicationStatus.APPLIED, "Seattle, WA", today.minusDays(12)),
                app(owner, "Lumen Analytics", "Platform Engineer", ApplicationStatus.INTERVIEWING, "Remote", today.minusDays(16)),
                app(owner, "Redwood Systems", "Associate SWE", ApplicationStatus.OFFER, "Denver, CO", today.minusDays(30)),
                app(owner, "Atlas Retail", "Full-Stack Engineer", ApplicationStatus.REJECTED, "Atlanta, GA", today.minusDays(21))));
    }

    private static JobApplication app(long owner, String company, String role, ApplicationStatus status,
                                      String location, LocalDate applied) {
        JobApplication a = new JobApplication(owner, company, role, status);
        a.setLocation(location);
        a.setAppliedDate(applied);
        return a;
    }
}
