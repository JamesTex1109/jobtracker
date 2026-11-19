package dev.portfolio.jobtracker.web.dto;

import dev.portfolio.jobtracker.application.ApplicationStatus;
import java.util.Map;

/** responseRate = (interviewing + offer + rejected) / submitted applications. */
public record StatsResponse(long total, Map<ApplicationStatus, Long> byStatus, double responseRate) {}
