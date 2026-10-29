package com.shoutoutz.api.magazine.infrastructure;

import com.shoutoutz.api.magazine.application.MagazineQueryRepository;
import com.shoutoutz.api.magazine.application.dto.MagazineCursor;
import com.shoutoutz.api.magazine.application.dto.MagazineItem;
import com.shoutoutz.api.magazine.domain.MagazineStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MagazineQueryRepositoryImpl implements MagazineQueryRepository {
    private static final String COLUMNS = """
            i.id, i.title, i.summary, i.thumbnail_media_id, i.status,
            i.published_at, i.created_at, i.updated_at, i.comment_count, i.view_count,
            CASE WHEN u.status = 'DELETED' THEN NULL ELSE u.id END AS featured_user_id,
            CASE WHEN u.status = 'DELETED' THEN NULL ELSE u.handle END AS handle,
            CASE WHEN u.status = 'DELETED' THEN '탈퇴한 사용자' ELSE p.display_name END AS display_name,
            CASE WHEN u.status = 'DELETED' THEN NULL ELSE i.featured_user_introduction END AS introduction,
            CASE WHEN u.status = 'DELETED' THEN NULL ELSE p.avatar_image_id END AS avatar_image_id,
            CASE WHEN author.status = 'DELETED' THEN NULL ELSE author.id END AS author_id,
            CASE WHEN author.status = 'DELETED' THEN NULL ELSE author.handle END AS author_handle,
            CASE WHEN author.status = 'DELETED' THEN '탈퇴한 사용자'
                 ELSE author_profile.display_name END AS author_display_name,
            CASE WHEN author.status = 'DELETED' THEN NULL
                 ELSE author_profile.avatar_image_id END AS author_avatar_image_id
            """;
    private static final String FROM = """
             FROM magazines i
             JOIN users u ON u.id = i.featured_user_id
             LEFT JOIN user_profiles p ON p.user_id = u.id
             JOIN users author ON author.id = i.author_id
             LEFT JOIN user_profiles author_profile ON author_profile.user_id = author.id
             WHERE i.deleted_at IS NULL
            """;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<MagazineItem> findPage(MagazineCursor cursor, int limit, boolean admin) {
        String orderColumn = "i.published_at";
        String visibility = " AND i.status = 'PUBLISHED'";
        if (admin) {
            orderColumn = "i.created_at";
            visibility = "";
        }
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + ", NULL AS content_md" + FROM + visibility);
        MapSqlParameterSource params = new MapSqlParameterSource("limit", limit);
        if (cursor != null) {
            sql.append(" AND (").append(orderColumn).append(", i.id) < (:timestamp, :id)");
            params.addValue("timestamp", Timestamp.from(cursor.timestamp())).addValue("id", cursor.id());
        }
        sql.append(" ORDER BY ").append(orderColumn).append(" DESC, i.id DESC LIMIT :limit");
        return jdbcTemplate.query(sql.toString(), params, this::map);
    }

    @Override
    public long count(boolean admin) {
        String sql = "SELECT count(*) FROM magazines WHERE deleted_at IS NULL";
        if (!admin) {
            sql += " AND status = 'PUBLISHED'";
        }
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Long.class);
    }

    @Override
    public Optional<MagazineItem> findPublished(long id) {
        List<MagazineItem> items = jdbcTemplate.query(
                "SELECT " + COLUMNS + ", i.content_md" + FROM + " AND i.status = 'PUBLISHED' AND i.id = :id",
                new MapSqlParameterSource("id", id), this::map);
        return items.stream().findFirst();
    }

    private MagazineItem map(ResultSet rs, int row) throws SQLException {
        return new MagazineItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("content_md"),
                rs.getObject("thumbnail_media_id", Long.class),
                rs.getObject("featured_user_id", Long.class),
                rs.getString("handle"),
                rs.getString("display_name"),
                rs.getString("introduction"),
                rs.getObject("avatar_image_id", Long.class),
                rs.getObject("author_id", Long.class),
                rs.getString("author_handle"),
                rs.getString("author_display_name"),
                rs.getObject("author_avatar_image_id", Long.class),
                MagazineStatus.valueOf(rs.getString("status")),
                instant(rs, "published_at"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"),
                rs.getLong("comment_count"),
                rs.getLong("view_count")
        );
    }

    private Instant instant(ResultSet rs, String name) throws SQLException {
        Timestamp value = rs.getTimestamp(name);
        if (value == null) {
            return null;
        }
        return value.toInstant();
    }
}
