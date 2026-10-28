package com.shoutoutz.api.jobposting.infrastructure.jpa;

import com.shoutoutz.api.jobposting.infrastructure.JobPostingEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobPostingJpaRepository extends JpaRepository<JobPostingEntity, Long> {

    @Query("SELECT jobPosting.id FROM JobPostingEntity jobPosting WHERE jobPosting.sourceUrl = :sourceUrl")
    Optional<Long> findIdBySourceUrl(@Param("sourceUrl") String sourceUrl);
}
