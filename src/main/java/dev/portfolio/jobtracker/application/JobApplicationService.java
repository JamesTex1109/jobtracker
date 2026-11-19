package dev.portfolio.jobtracker.application;

import dev.portfolio.jobtracker.web.dto.JobApplicationRequest;
import dev.portfolio.jobtracker.web.dto.StatsResponse;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JobApplicationService {

    private final JobApplicationRepository repository;

    public JobApplicationService(JobApplicationRepository repository) {
        this.repository = repository;
    }

    public Page<JobApplication> search(ApplicationStatus status, String text, Pageable pageable) {
        Specification<JobApplication> spec = Specification.allOf(
                JobApplicationSpecs.hasStatus(status),
                JobApplicationSpecs.matches(text));
        return repository.findAll(spec, pageable);
    }

    public JobApplication get(long id) {
        return repository.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    @Transactional
    public JobApplication create(JobApplicationRequest request) {
        JobApplication app = new JobApplication(request.company(), request.role(), ApplicationStatus.WISHLIST);
        apply(app, request);
        return repository.save(app);
    }

    @Transactional
    public JobApplication update(long id, JobApplicationRequest request) {
        JobApplication app = get(id);
        app.setCompany(request.company());
        app.setRole(request.role());
        apply(app, request);
        return app;
    }

    @Transactional
    public JobApplication changeStatus(long id, ApplicationStatus status) {
        JobApplication app = get(id);
        app.changeStatus(status);
        return app;
    }

    @Transactional
    public void delete(long id) {
        repository.delete(get(id));
    }

    public StatsResponse stats() {
        Map<ApplicationStatus, Long> byStatus = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus s : ApplicationStatus.values()) {
            byStatus.put(s, 0L);
        }
        repository.countByStatus().forEach(c -> byStatus.put(c.status(), c.count()));

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long submitted = total - byStatus.get(ApplicationStatus.WISHLIST);
        long responses = byStatus.entrySet().stream()
                .filter(e -> e.getKey().isResponse())
                .mapToLong(Map.Entry::getValue)
                .sum();
        double responseRate = submitted == 0 ? 0.0 : (double) responses / submitted;
        return new StatsResponse(total, byStatus, Math.round(responseRate * 1000) / 1000.0);
    }

    private static void apply(JobApplication app, JobApplicationRequest request) {
        app.setLocation(request.location());
        app.setJobUrl(request.jobUrl());
        app.setNotes(request.notes());
        if (request.appliedDate() != null) {
            app.setAppliedDate(request.appliedDate());
        }
        if (request.status() != null) {
            app.changeStatus(request.status());
        }
    }
}
