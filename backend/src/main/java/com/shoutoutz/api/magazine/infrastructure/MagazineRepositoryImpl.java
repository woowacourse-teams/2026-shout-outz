package com.shoutoutz.api.magazine.infrastructure;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.magazine.domain.Magazine;
import com.shoutoutz.api.magazine.domain.MagazineErrorCode;
import com.shoutoutz.api.magazine.domain.MagazineRepository;
import com.shoutoutz.api.magazine.infrastructure.jpa.MagazineJpaRepository;
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
public class MagazineRepositoryImpl implements MagazineRepository {
    private final MagazineJpaRepository magazineJpaRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Magazine save(Magazine magazine) {
        if (magazine.getId() == null) {
            return magazineJpaRepository.saveAndFlush(MagazineEntity.from(magazine)).toDomain();
        }
        MagazineEntity entity = magazineJpaRepository.findByIdAndDeletedAtIsNull(magazine.getId())
                .orElseThrow(() -> new EntityNotFoundException(MagazineErrorCode.MAGAZINE_NOT_FOUND));
        entity.update(magazine);
        magazineJpaRepository.flush();
        return entity.toDomain();
    }

    @Override
    public Optional<Magazine> findActiveById(long id) {
        return magazineJpaRepository.findByIdAndDeletedAtIsNull(id).map(MagazineEntity::toDomain);
    }

    @Override
    public Optional<Magazine> findForUpdate(long id) {
        return magazineJpaRepository.findForUpdate(id).map(MagazineEntity::toDomain);
    }

    @Override
    public long recordView(long magazineId, VisitorKey visitorKey, LocalDate viewedOn, Instant viewedAt) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("magazineId", magazineId)
                .addValue("visitorKeyHash", visitorKey.hash())
                .addValue("viewedOn", viewedOn)
                .addValue("viewedAt", Timestamp.from(viewedAt));
        jdbcTemplate.update(
                """
                        WITH recorded AS (
                            INSERT INTO magazine_view_days (magazine_id, visitor_key_hash, viewed_on, first_seen_at)
                            VALUES (:magazineId, :visitorKeyHash, :viewedOn, :viewedAt)
                            ON CONFLICT (magazine_id, visitor_key_hash, viewed_on) DO NOTHING
                            RETURNING magazine_id
                        )
                        UPDATE magazines
                        SET view_count = view_count + 1
                        WHERE id IN (SELECT magazine_id FROM recorded)
                        """,
                parameters
        );
        // 중복 INSERT가 다른 트랜잭션을 기다렸다면, 다음 문장의 새 스냅샷으로 커밋된 조회수를 읽는다.
        return jdbcTemplate.queryForList(
                """
                        SELECT view_count FROM magazines
                        WHERE id = :magazineId AND status = 'PUBLISHED' AND deleted_at IS NULL
                        """,
                parameters,
                Long.class
        ).stream().findFirst()
                .orElseThrow(() -> new EntityNotFoundException(MagazineErrorCode.MAGAZINE_NOT_FOUND));
    }
}
