package dev.portfolio.jobtracker.web.dto;

import dev.portfolio.jobtracker.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull ApplicationStatus status) {}
