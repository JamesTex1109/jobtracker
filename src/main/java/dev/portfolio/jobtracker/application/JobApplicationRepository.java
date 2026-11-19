package dev.portfolio.jobtracker.application;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface JobApplicationRepository
        extends JpaRepository<JobApplication, Long>, JpaSpecificationExecutor<JobApplication> {

    Optional<JobApplication> findByIdAndOwnerId(long id, long ownerId);

    @Modifying
    @Query("delete from JobApplication a where a.ownerId = :ownerId")
    void deleteAllByOwnerId(long ownerId);

    @Query("""
            select new dev.portfolio.jobtracker.application.StatusCount(a.status, count(a))
            from JobApplication a
            where a.ownerId = :ownerId
            group by a.status
            """)
    List<StatusCount> countByStatus(long ownerId);
}
