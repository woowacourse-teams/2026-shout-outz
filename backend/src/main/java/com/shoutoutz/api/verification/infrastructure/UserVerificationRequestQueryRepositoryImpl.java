package com.shoutoutz.api.verification.infrastructure;

import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.verification.application.AdminVerificationRequestQueryRepository;
import com.shoutoutz.api.verification.application.dto.AdminVerificationRequestCursor;
import com.shoutoutz.api.verification.application.dto.AdminVerificationRequestItem;
import com.shoutoutz.api.verification.application.dto.AdminVerificationRequestPage;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserVerificationRequestQueryRepositoryImpl
        implements AdminVerificationRequestQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public AdminVerificationRequestPage findAll(
            VerificationRequestStatus status,
            AdminVerificationRequestCursor cursor,
            int size
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    r.id AS request_id,
                    r.user_id,
                    u.handle,
                    r.user_type,
                    r.nickname,
                    r.cohort,
                    r.track,
                    r.status,
                    r.requested_at
                FROM user_verification_requests r
                JOIN users u ON u.id = r.user_id
                WHERE r.status = :status
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("status", status.name())
                .addValue("limit", size + 1);

        if (cursor != null) {
            sql.append("AND (r.requested_at, r.id) < (:cursorRequestedAt, :cursorRequestId)\n");
            parameters
                    .addValue(
                            "cursorRequestedAt",
                            OffsetDateTime.ofInstant(cursor.requestedAt(), ZoneOffset.UTC)
                    )
                    .addValue("cursorRequestId", cursor.requestId());
        }
        sql.append("ORDER BY r.requested_at DESC, r.id DESC\n");
        sql.append("LIMIT :limit");

        List<AdminVerificationRequestItem> items = jdbcTemplate.query(
                sql.toString(),
                parameters,
                (resultSet, rowNumber) -> new AdminVerificationRequestItem(
                        resultSet.getLong("request_id"),
                        resultSet.getLong("user_id"),
                        resultSet.getString("handle"),
                        UserType.valueOf(resultSet.getString("user_type")),
                        resultSet.getString("nickname"),
                        resultSet.getObject("cohort", Integer.class),
                        resultSet.getString("track"),
                        VerificationRequestStatus.valueOf(resultSet.getString("status")),
                        resultSet.getObject("requested_at", OffsetDateTime.class).toInstant()
                )
        );
        return createPage(items, size, countAll(status));
    }

    private long countAll(VerificationRequestStatus status) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM user_verification_requests
                        WHERE status = :status
                        """,
                new MapSqlParameterSource("status", status.name()),
                Long.class
        );
    }

    private AdminVerificationRequestPage createPage(
            List<AdminVerificationRequestItem> items,
            int size,
            long totalCount
    ) {
        if (items.size() <= size) {
            return new AdminVerificationRequestPage(items, false, totalCount);
        }
        return new AdminVerificationRequestPage(items.subList(0, size), true, totalCount);
    }
}
