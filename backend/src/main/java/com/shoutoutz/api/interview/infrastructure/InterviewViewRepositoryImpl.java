package com.shoutoutz.api.interview.infrastructure;

import com.shoutoutz.api.interview.domain.InterviewViewRepository;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 조회 기록(interview_view_days)과 조회수(interviews.view_count)를 함께 다룬다.
 * 조회수는 콘텐츠 수정이 아니므로 updated_at 은 바꾸지 않는다.
 */
@Repository
@RequiredArgsConstructor
public class InterviewViewRepositoryImpl implements InterviewViewRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * 기록, 조회수 증가, 조회수 조회를 한 문장으로 처리한다.
     * 같은 방문자의 요청이 동시에 들어와도 기본 키 충돌로 한 건만 INSERT 되고, 조회수도 그 한 번만 오른다.
     * 한 문장 안의 SELECT 는 같은 문장의 UPDATE 이전 값을 보므로, 올린 값(updated)을 먼저 쓰고
     * 이미 기록돼 있어 올리지 않았을 때만 현재 값을 쓴다.
     */
    @Override
    public long record(long interviewId, VisitorKey visitorKey, LocalDate viewedOn, Instant viewedAt) {
        Long viewCount = jdbcTemplate.queryForObject(
                """
                        WITH recorded AS (
                            INSERT INTO interview_view_days (interview_id, visitor_key_hash, viewed_on, first_seen_at)
                            VALUES (:interviewId, :visitorKeyHash, :viewedOn, :viewedAt)
                            ON CONFLICT (interview_id, visitor_key_hash, viewed_on) DO NOTHING
                            RETURNING interview_id
                        ),
                        updated AS (
                            UPDATE interviews
                            SET view_count = view_count + 1
                            WHERE id IN (SELECT interview_id FROM recorded)
                            RETURNING view_count
                        )
                        SELECT COALESCE(
                            (SELECT view_count FROM updated),
                            (SELECT view_count FROM interviews WHERE id = :interviewId)
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("interviewId", interviewId)
                        .addValue("visitorKeyHash", visitorKey.hash())
                        .addValue("viewedOn", viewedOn)
                        .addValue("viewedAt", Timestamp.from(viewedAt)),
                Long.class
        );
        return viewCount;
    }
}
