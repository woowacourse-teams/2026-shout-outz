package com.shoutoutz.api.interview.infrastructure;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.interview.domain.Interview;
import com.shoutoutz.api.interview.domain.InterviewErrorCode;
import com.shoutoutz.api.interview.domain.InterviewRepository;
import com.shoutoutz.api.interview.infrastructure.jpa.InterviewJpaRepository;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InterviewRepositoryImpl implements InterviewRepository {
    private final InterviewJpaRepository interviewJpaRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Interview save(Interview interview) {
        if (interview.getId() == null) {
            return interviewJpaRepository.saveAndFlush(InterviewEntity.from(interview)).toDomain();
        }
        InterviewEntity entity = interviewJpaRepository.findByIdAndDeletedAtIsNull(interview.getId())
                .orElseThrow(() -> new EntityNotFoundException(InterviewErrorCode.INTERVIEW_NOT_FOUND));
        entity.update(interview);
        interviewJpaRepository.flush();
        return entity.toDomain();
    }

    @Override
    public Optional<Interview> findActiveById(long id) {
        return interviewJpaRepository.findByIdAndDeletedAtIsNull(id).map(InterviewEntity::toDomain);
    }

    @Override
    public Optional<Interview> findForUpdate(long id) {
        return interviewJpaRepository.findForUpdate(id).map(InterviewEntity::toDomain);
    }

    @Override
    public long recordView(long interviewId, VisitorKey visitorKey, LocalDate viewedOn, Instant viewedAt) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("interviewId", interviewId)
                .addValue("visitorKeyHash", visitorKey.hash())
                .addValue("viewedOn", viewedOn)
                .addValue("viewedAt", Timestamp.from(viewedAt));
        jdbcTemplate.update(
                """
                        WITH recorded AS (
                            INSERT INTO interview_view_days (interview_id, visitor_key_hash, viewed_on, first_seen_at)
                            VALUES (:interviewId, :visitorKeyHash, :viewedOn, :viewedAt)
                            ON CONFLICT (interview_id, visitor_key_hash, viewed_on) DO NOTHING
                            RETURNING interview_id
                        )
                        UPDATE interviews
                        SET view_count = view_count + 1
                        WHERE id IN (SELECT interview_id FROM recorded)
                        """,
                parameters
        );
        // 중복 INSERT가 다른 트랜잭션을 기다렸다면, 다음 문장의 새 스냅샷으로 커밋된 조회수를 읽는다.
        return jdbcTemplate.queryForList(
                """
                        SELECT view_count FROM interviews
                        WHERE id = :interviewId AND status = 'PUBLISHED' AND deleted_at IS NULL
                        """,
                parameters,
                Long.class
        ).stream().findFirst()
                .orElseThrow(() -> new EntityNotFoundException(InterviewErrorCode.INTERVIEW_NOT_FOUND));
    }
}
