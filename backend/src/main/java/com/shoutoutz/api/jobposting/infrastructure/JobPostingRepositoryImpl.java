package com.shoutoutz.api.jobposting.infrastructure;

import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_DUPLICATE_URL;

import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.JobPostingRepository;
import com.shoutoutz.api.jobposting.domain.JobPostingUrl;
import com.shoutoutz.api.jobposting.infrastructure.jpa.JobPostingJpaRepository;
import com.shoutoutz.api.jobposting.infrastructure.jpa.JobRequirementJpaRepository;
import com.shoutoutz.api.jobposting.infrastructure.mapper.JobPostingMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JobPostingRepositoryImpl implements JobPostingRepository {

    private static final String SOURCE_URL_UNIQUE_CONSTRAINT = "uq_job_postings_source_url";

    private final JobPostingJpaRepository jobPostingJpaRepository;
    private final JobRequirementJpaRepository jobRequirementJpaRepository;

    @Override
    public JobPosting save(JobPosting jobPosting) {
        JobPostingEntity savedPosting = savePosting(jobPosting);
        List<JobRequirementEntity> savedRequirements = jobRequirementJpaRepository.saveAll(
                JobPostingMapper.toRequirementEntities(savedPosting.getId(), jobPosting));
        return JobPostingMapper.toDomain(savedPosting, savedRequirements);
    }

    @Override
    public Optional<JobPosting> findById(long jobPostingId) {
        return jobPostingJpaRepository.findById(jobPostingId)
                .map(posting -> JobPostingMapper.toDomain(
                        posting,
                        jobRequirementJpaRepository.findAllByJobPostingIdOrderByDisplayOrderAsc(jobPostingId)));
    }

    @Override
    public Optional<Long> findIdBySourceUrl(JobPostingUrl sourceUrl) {
        return jobPostingJpaRepository.findIdBySourceUrl(sourceUrl.value());
    }

    /**
     * 동시 요청이 사전 검사(findIdBySourceUrl)를 함께 통과하면 UNIQUE 제약에 걸린다.
     * 이 경우에도 409 로 응답하도록 중복 예외로 변환한다.
     * id 를 IDENTITY 로 받으므로 INSERT 가 save 시점에 바로 나가 제약 위반도 이 자리에서 잡힌다.
     */
    private JobPostingEntity savePosting(JobPosting jobPosting) {
        try {
            return jobPostingJpaRepository.save(JobPostingMapper.toEntity(jobPosting));
        } catch (DataIntegrityViolationException e) {
            if (isUniqueViolation(e, SOURCE_URL_UNIQUE_CONSTRAINT)) {
                throw new DuplicateEntityException(JOB_POSTING_DUPLICATE_URL, e);
            }
            throw e;
        }
    }

    private boolean isUniqueViolation(DataIntegrityViolationException e, String constraintName) {
        return e.getCause() instanceof ConstraintViolationException cause
                && constraintName.equals(cause.getConstraintName());
    }
}
