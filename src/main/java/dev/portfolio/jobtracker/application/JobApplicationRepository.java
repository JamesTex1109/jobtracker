package dev.portfolio.jobtracker.application;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface JobApplicationRepository
        extends JpaRepository<JobApplication, Long>, JpaSpecificationExecutor<JobApplication> {

    @Query("""
            select new dev.portfolio.jobtracker.application.StatusCount(a.status, count(a))
            from JobApplication a
            group by a.status
            """)
    List<StatusCount> countByStatus();
}
