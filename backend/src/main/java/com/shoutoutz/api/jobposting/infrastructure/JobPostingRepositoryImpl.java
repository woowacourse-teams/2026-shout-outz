package com.shoutoutz.api.jobposting.infrastructure;

import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_DUPLICATE_URL;

import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.JobPostingRepository;
import com.shoutoutz.api.jobposting.domain.JobPostingUrl;
import com.shoutoutz.api.jobposting.domain.JobPostingVisibility;
import com.shoutoutz.api.jobposting.infrastructure.jpa.JobPostingJpaRepository;
import com.shoutoutz.api.jobposting.infrastructure.jpa.JobRequirementJpaRepository;
import com.shoutoutz.api.jobposting.infrastructure.mapper.JobPostingMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JobPostingRepositoryImpl implements JobPostingRepository {

    private static final String SOURCE_URL_UNIQUE_CONSTRAINT = "uq_job_postings_source_url";

    private final JobPostingJpaRepository jobPostingJpaRepository;
    private final JobRequirementJpaRepository jobRequirementJpaRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

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
     * 엔티티를 저장하면 Worker 가 SQL 로 바꾼 분석 상태까지 덮어쓰므로, 숨김 시각만 한 문장으로 바꾼다.
     */
    @Override
    public Optional<JobPostingVisibility> updateHiddenAt(long jobPostingId, boolean hidden, Instant now) {
        List<JobPostingVisibility> updated = jdbcTemplate.query("""
                UPDATE job_postings
                SET hidden_at = CASE WHEN :hidden THEN COALESCE(hidden_at, :now) ELSE NULL END,
                    updated_at = :now
                WHERE id = :id
                RETURNING id, hidden_at
                """, new MapSqlParameterSource()
                        .addValue("id", jobPostingId)
                        .addValue("hidden", hidden)
                        .addValue("now", Timestamp.from(now)),
                (rs, rowNum) -> {
                    Timestamp hiddenAt = rs.getTimestamp("hidden_at");
                    return new JobPostingVisibility(rs.getLong("id"), hiddenAt == null ? null : hiddenAt.toInstant());
                });
        return updated.stream().findFirst();
    }

    /**
     * 역량 행을 먼저 잠근 뒤 공고를 지운다.
     * Worker 는 역량 행, 공고 행 순서로 잠그는데, 공고부터 지우면 공고 행을 잡은 채 역량 행을 기다리게 되어 서로 기다리다 멈출 수 있다.
     * 역량과 추천 자료는 외래 키의 ON DELETE CASCADE 로 함께 지워진다.
     */
    @Override
    @Transactional
    public boolean deleteById(long jobPostingId) {
        lockRequirements(jobPostingId);
        return jdbcTemplate.update("DELETE FROM job_postings WHERE id = :id",
                new MapSqlParameterSource("id", jobPostingId)) == 1;
    }

    /**
     * 삭제와 같은 이유로 역량 행, 공고 행 순서로 잠근다.
     * 역량은 잠근 뒤에 읽어야 Worker 가 막 저장한 상태를 본다.
     */
    @Override
    @Transactional
    public Optional<JobPosting> findByIdForUpdate(long jobPostingId) {
        lockRequirements(jobPostingId);
        return jobPostingJpaRepository.findForUpdate(jobPostingId)
                .map(posting -> JobPostingMapper.toDomain(
                        posting,
                        jobRequirementJpaRepository.findAllByJobPostingIdOrderByDisplayOrderAsc(jobPostingId)));
    }

    /**
     * 공고 칸은 잠근 뒤 읽은 값에 변경을 더한 것이라 엔티티로 그대로 저장한다.
     * 역량은 같은 순서 번호가 겹치지 않도록 먼저 지운 뒤 넣는다.
     */
    @Override
    @Transactional
    public void update(JobPosting jobPosting, boolean replaceRequirements) {
        jobPostingJpaRepository.save(JobPostingMapper.toEntity(jobPosting));
        if (!replaceRequirements) {
            return;
        }
        jdbcTemplate.update("DELETE FROM job_requirements WHERE job_posting_id = :id",
                new MapSqlParameterSource("id", jobPosting.getId()));
        jobRequirementJpaRepository.saveAll(JobPostingMapper.toRequirementEntities(jobPosting.getId(), jobPosting));
    }

    /**
     * 조회 결과는 쓰지 않는다. 트랜잭션이 끝날 때까지 역량 행을 잡아 두는 것이 목적이다.
     */
    private void lockRequirements(long jobPostingId) {
        jdbcTemplate.queryForList(
                "SELECT id FROM job_requirements WHERE job_posting_id = :id ORDER BY id FOR UPDATE",
                new MapSqlParameterSource("id", jobPostingId), Long.class);
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
