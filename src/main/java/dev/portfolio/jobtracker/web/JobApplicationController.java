package dev.portfolio.jobtracker.web;

import dev.portfolio.jobtracker.application.ApplicationStatus;
import dev.portfolio.jobtracker.application.JobApplication;
import dev.portfolio.jobtracker.application.JobApplicationService;
import dev.portfolio.jobtracker.web.dto.JobApplicationRequest;
import dev.portfolio.jobtracker.web.dto.JobApplicationResponse;
import dev.portfolio.jobtracker.web.dto.PageResponse;
import dev.portfolio.jobtracker.web.dto.StatsResponse;
import dev.portfolio.jobtracker.web.dto.StatusUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<JobApplicationResponse> list(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("updatedAt")));
        return PageResponse.from(service.search(status, q, pageable), JobApplicationResponse::from);
    }

    @GetMapping("/{id}")
    public JobApplicationResponse get(@PathVariable long id) {
        return JobApplicationResponse.from(service.get(id));
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return service.stats();
    }

    @PostMapping
    public ResponseEntity<JobApplicationResponse> create(@Valid @RequestBody JobApplicationRequest request) {
        JobApplication created = service.create(request);
        return ResponseEntity.created(URI.create("/api/applications/" + created.getId()))
                .body(JobApplicationResponse.from(created));
    }

    @PutMapping("/{id}")
    public JobApplicationResponse update(@PathVariable long id, @Valid @RequestBody JobApplicationRequest request) {
        return JobApplicationResponse.from(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public JobApplicationResponse changeStatus(@PathVariable long id, @Valid @RequestBody StatusUpdateRequest request) {
        return JobApplicationResponse.from(service.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
