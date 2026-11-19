package dev.portfolio.jobtracker.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning user's id. Kept as a plain column: nothing here needs to load the user itself. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long ownerId;

    @Column(nullable = false, length = 120)
    private String company;

    @Column(nullable = false, length = 120)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(length = 120)
    private String location;

    @Column(name = "job_url", length = 500)
    private String jobUrl;

    @Column(name = "applied_date")
    private LocalDate appliedDate;

    @Column(length = 2000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JobApplication() {
        // for JPA
    }

    public JobApplication(long ownerId, String company, String role, ApplicationStatus status) {
        this.ownerId = ownerId;
        this.company = company;
        this.role = role;
        this.status = status;
    }

    /** Moves to a new status, stamping the applied date the first time it's submitted. */
    public void changeStatus(ApplicationStatus newStatus) {
        if (appliedDate == null && newStatus != ApplicationStatus.WISHLIST) {
            appliedDate = LocalDate.now();
        }
        this.status = newStatus;
    }

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public String getCompany() { return company; }
    public String getRole() { return role; }
    public ApplicationStatus getStatus() { return status; }
    public String getLocation() { return location; }
    public String getJobUrl() { return jobUrl; }
    public LocalDate getAppliedDate() { return appliedDate; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setCompany(String company) { this.company = company; }
    public void setRole(String role) { this.role = role; }
    public void setLocation(String location) { this.location = location; }
    public void setJobUrl(String jobUrl) { this.jobUrl = jobUrl; }
    public void setAppliedDate(LocalDate appliedDate) { this.appliedDate = appliedDate; }
    public void setNotes(String notes) { this.notes = notes; }
}
