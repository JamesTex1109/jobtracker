package dev.portfolio.jobtracker.web.dto;

import dev.portfolio.jobtracker.application.ApplicationStatus;
import dev.portfolio.jobtracker.application.JobApplication;
import java.time.Instant;
import java.time.LocalDate;

public record JobApplicationResponse(
        Long id,
        String company,
        String role,
        ApplicationStatus status,
        String location,
        String jobUrl,
        LocalDate appliedDate,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    public static JobApplicationResponse from(JobApplication a) {
        return new JobApplicationResponse(
                a.getId(), a.getCompany(), a.getRole(), a.getStatus(), a.getLocation(),
                a.getJobUrl(), a.getAppliedDate(), a.getNotes(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
