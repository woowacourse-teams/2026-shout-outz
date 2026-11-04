package com.shoutoutz.api.jobposting.infrastructure.jpa;

import com.shoutoutz.api.jobposting.infrastructure.JobPostingEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobPostingJpaRepository extends JpaRepository<JobPostingEntity, Long> {

    @Query("SELECT jobPosting.id FROM JobPostingEntity jobPosting WHERE jobPosting.sourceUrl = :sourceUrl")
    Optional<Long> findIdBySourceUrl(@Param("sourceUrl") String sourceUrl);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT jobPosting FROM JobPostingEntity jobPosting WHERE jobPosting.id = :id")
    Optional<JobPostingEntity> findForUpdate(@Param("id") long id);
}
