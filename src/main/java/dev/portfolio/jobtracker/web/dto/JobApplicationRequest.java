package dev.portfolio.jobtracker.web.dto;

import dev.portfolio.jobtracker.application.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record JobApplicationRequest(
        @NotBlank @Size(max = 120) String company,
        @NotBlank @Size(max = 120) String role,
        ApplicationStatus status,
        @Size(max = 120) String location,
        @Size(max = 500) @Pattern(regexp = "^https?://.*", message = "must start with http:// or https://") String jobUrl,
        @PastOrPresent LocalDate appliedDate,
        @Size(max = 2000) String notes) {}
