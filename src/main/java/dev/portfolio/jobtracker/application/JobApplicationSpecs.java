package dev.portfolio.jobtracker.application;

import org.springframework.data.jpa.domain.Specification;

/** Composable query filters, applied only when the caller supplies a value. */
final class JobApplicationSpecs {

    private JobApplicationSpecs() {}

    static Specification<JobApplication> ownedBy(long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("ownerId"), ownerId);
    }

    static Specification<JobApplication> hasStatus(ApplicationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    /** Case-insensitive match against company or role. */
    static Specification<JobApplication> matches(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            String pattern = "%" + text.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("company")), pattern),
                    cb.like(cb.lower(root.get("role")), pattern));
        };
    }
}
