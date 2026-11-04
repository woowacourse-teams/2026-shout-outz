package com.shoutoutz.api.jobposting.infrastructure.jpa;

import com.shoutoutz.api.jobposting.infrastructure.JobRequirementEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRequirementJpaRepository extends JpaRepository<JobRequirementEntity, Long> {

    List<JobRequirementEntity> findAllByJobPostingIdOrderByDisplayOrderAsc(long jobPostingId);
}
