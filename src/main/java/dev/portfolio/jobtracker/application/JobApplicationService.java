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

/**
 * Every method takes the caller's user id and only touches that user's rows. Another user's
 * application is reported as "not found" so ids can't be probed to learn what exists.
 */
@Service
@Transactional(readOnly = true)
public class JobApplicationService {

    private final JobApplicationRepository repository;

    public JobApplicationService(JobApplicationRepository repository) {
        this.repository = repository;
    }

    public Page<JobApplication> search(long ownerId, ApplicationStatus status, String text, Pageable pageable) {
        Specification<JobApplication> spec = Specification.allOf(
                JobApplicationSpecs.ownedBy(ownerId),
                JobApplicationSpecs.hasStatus(status),
                JobApplicationSpecs.matches(text));
        return repository.findAll(spec, pageable);
    }

    public JobApplication get(long ownerId, long id) {
        return repository.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    @Transactional
    public JobApplication create(long ownerId, JobApplicationRequest request) {
        JobApplication app = new JobApplication(ownerId, request.company(), request.role(), ApplicationStatus.WISHLIST);
        apply(app, request);
        return repository.save(app);
    }

    @Transactional
    public JobApplication update(long ownerId, long id, JobApplicationRequest request) {
        JobApplication app = get(ownerId, id);
        app.setCompany(request.company());
        app.setRole(request.role());
        apply(app, request);
        return app;
    }

    @Transactional
    public JobApplication changeStatus(long ownerId, long id, ApplicationStatus status) {
        JobApplication app = get(ownerId, id);
        app.changeStatus(status);
        return app;
    }

    @Transactional
    public void delete(long ownerId, long id) {
        repository.delete(get(ownerId, id));
    }

    public StatsResponse stats(long ownerId) {
        Map<ApplicationStatus, Long> byStatus = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus s : ApplicationStatus.values()) {
            byStatus.put(s, 0L);
        }
        repository.countByStatus(ownerId).forEach(c -> byStatus.put(c.status(), c.count()));

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
