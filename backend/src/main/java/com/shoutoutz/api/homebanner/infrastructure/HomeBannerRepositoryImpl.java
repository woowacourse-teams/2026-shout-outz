package com.shoutoutz.api.homebanner.infrastructure;

import com.shoutoutz.api.homebanner.domain.BannerDestinationType;
import com.shoutoutz.api.homebanner.domain.BannerLinkType;
import com.shoutoutz.api.homebanner.domain.BannerTargetType;
import com.shoutoutz.api.homebanner.domain.HomeBanner;
import com.shoutoutz.api.homebanner.domain.HomeBannerRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HomeBannerRepositoryImpl implements HomeBannerRepository {

    private static final String COLUMNS = """
            id, media_id, destination_type, target_type, target_id,
            link_type, link_url, display_order, active, created_by, created_at, updated_at
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public HomeBanner save(HomeBanner homeBanner) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO home_banners (
                            media_id, destination_type, target_type, target_id,
                            link_type, link_url, display_order, active, created_by
                        ) VALUES (
                            :mediaId, :destinationType, :targetType, :targetId,
                            :linkType, :linkUrl, :displayOrder, :active, :createdBy
                        )
                        RETURNING %s
                        """.formatted(COLUMNS),
                parameters(homeBanner),
                (resultSet, rowNumber) -> toHomeBanner(resultSet)
        );
    }

    @Override
    public Optional<HomeBanner> findById(long bannerId) {
        return findOne("SELECT %s FROM home_banners WHERE id = :bannerId".formatted(COLUMNS), bannerId);
    }

    @Override
    public List<HomeBanner> findAll() {
        return jdbcTemplate.query(
                "SELECT %s FROM home_banners ORDER BY display_order, id".formatted(COLUMNS),
                Map.of(),
                (resultSet, rowNumber) -> toHomeBanner(resultSet)
        );
    }

    @Override
    public List<HomeBanner> findActive(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        // 예약 노출 도입 시 active와 함께 start_at/end_at 범위를 검사한다.
        return jdbcTemplate.query(
                """
                        SELECT %s
                        FROM home_banners
                        WHERE active = true
                        ORDER BY display_order, id
                        LIMIT :limit
                        """.formatted(COLUMNS),
                Map.of("limit", limit),
                (resultSet, rowNumber) -> toHomeBanner(resultSet)
        );
    }

    @Override
    public Optional<HomeBanner> update(HomeBanner homeBanner) {
        List<HomeBanner> updated = jdbcTemplate.query(
                """
                        UPDATE home_banners
                        SET media_id = :mediaId,
                            destination_type = :destinationType,
                            target_type = :targetType,
                            target_id = :targetId,
                            link_type = :linkType,
                            link_url = :linkUrl,
                            display_order = :displayOrder,
                            active = :active,
                            updated_at = now()
                        WHERE id = :bannerId
                        RETURNING %s
                        """.formatted(COLUMNS),
                parameters(homeBanner).addValue("bannerId", homeBanner.getId()),
                (resultSet, rowNumber) -> toHomeBanner(resultSet)
        );
        return updated.stream().findFirst();
    }

    @Override
    public boolean deleteById(long bannerId) {
        return jdbcTemplate.update(
                "DELETE FROM home_banners WHERE id = :bannerId",
                Map.of("bannerId", bannerId)
        ) == 1;
    }

    private Optional<HomeBanner> findOne(String sql, long bannerId) {
        List<HomeBanner> banners = jdbcTemplate.query(
                sql,
                Map.of("bannerId", bannerId),
                (resultSet, rowNumber) -> toHomeBanner(resultSet)
        );
        return banners.stream().findFirst();
    }

    private MapSqlParameterSource parameters(HomeBanner homeBanner) {
        return new MapSqlParameterSource()
                .addValue("mediaId", homeBanner.getMediaId())
                .addValue("destinationType", homeBanner.getDestinationType().name())
                .addValue("targetType", nameOrNull(homeBanner.getTargetType()))
                .addValue("targetId", homeBanner.getTargetId())
                .addValue("linkType", nameOrNull(homeBanner.getLinkType()))
                .addValue("linkUrl", homeBanner.getLinkUrl())
                .addValue("displayOrder", homeBanner.getDisplayOrder())
                .addValue("active", homeBanner.isActive())
                .addValue("createdBy", homeBanner.getCreatedBy());
    }

    private String nameOrNull(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private HomeBanner toHomeBanner(ResultSet resultSet) throws SQLException {
        return HomeBanner.reconstitute(
                resultSet.getLong("id"),
                resultSet.getLong("media_id"),
                BannerDestinationType.valueOf(resultSet.getString("destination_type")),
                enumOrNull(BannerTargetType.class, resultSet.getString("target_type")),
                resultSet.getObject("target_id", Long.class),
                enumOrNull(BannerLinkType.class, resultSet.getString("link_type")),
                resultSet.getString("link_url"),
                resultSet.getInt("display_order"),
                resultSet.getBoolean("active"),
                resultSet.getLong("created_by"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private <T extends Enum<T>> T enumOrNull(Class<T> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }
}
