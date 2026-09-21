package com.shoutoutz.api.home.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.home.application.HomeStatisticsQueryRepository;
import com.shoutoutz.api.home.application.dto.HomeStatisticsCounts;
import com.shoutoutz.api.news.domain.enums.NewsType;
import com.shoutoutz.api.news.infrastructure.NewsEntity;
import com.shoutoutz.api.news.infrastructure.jpa.NewsJpaRepository;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class HomeStatisticsQueryRepositoryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");

    @Autowired
    private HomeStatisticsQueryRepository homeStatisticsQueryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NewsJpaRepository newsJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("홈 통계는 명세의 상태 조건과 이벤트 경계 시각을 적용한다")
    void findsHomeStatistics() {
        HomeStatisticsCounts before = homeStatisticsQueryRepository.find(NOW);
        User user = userRepository.save(User.initialize("home-statistics-user"));
        saveProject("approved-active", "APPROVED", null);
        saveProject("pending-active", "PENDING", null);
        saveProject("approved-deleted", "APPROVED", NOW);
        saveFeed(user.getId(), null);
        saveFeed(user.getId(), NOW);
        saveEvent(user.getId(), NOW, NOW);
        saveEvent(user.getId(), NOW.plusSeconds(1), NOW.plusSeconds(2));
        saveEvent(user.getId(), NOW.minusSeconds(2), NOW.minusSeconds(1));
        NewsEntity deletedOngoingEvent = saveEvent(user.getId(), NOW.minusSeconds(1), NOW.plusSeconds(1));
        jdbcTemplate.update(
                "UPDATE news SET deleted_at = ? WHERE id = ?",
                Timestamp.from(NOW),
                deletedOngoingEvent.getId()
        );
        saveNotice(user.getId());

        HomeStatisticsCounts counts = homeStatisticsQueryRepository.find(NOW);

        assertThat(counts).isEqualTo(new HomeStatisticsCounts(
                before.projectCount() + 1,
                before.feedCount() + 1,
                before.ongoingEventCount() + 1
        ));
    }

    private void saveProject(String slug, String approvalStatus, Instant deletedAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO projects (
                            cohort, team_name, slug, title, tagline, github_repository_url,
                            service_status, approval_status, deleted_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, 'OPERATING', ?, ?)
                        """,
                8,
                "샤라웃즈",
                slug + "-" + UUID.randomUUID(),
                "홈 통계 프로젝트",
                "홈 통계 테스트용 프로젝트",
                "https://github.com/woowacourse-teams/home-statistics-" + UUID.randomUUID(),
                approvalStatus,
                deletedAt == null ? null : Timestamp.from(deletedAt)
        );
    }

    private void saveFeed(long authorId, Instant deletedAt) {
        jdbcTemplate.update(
                "INSERT INTO feeds (author_id, content, deleted_at) VALUES (?, ?, ?)",
                authorId,
                "홈 통계 테스트 피드",
                deletedAt == null ? null : Timestamp.from(deletedAt)
        );
    }

    private NewsEntity saveEvent(long authorId, Instant startAt, Instant endAt) {
        return newsJpaRepository.saveAndFlush(NewsEntity.builder()
                .type(NewsType.EVENT)
                .title("홈 통계 이벤트")
                .summary("홈 통계 이벤트 요약")
                .body("홈 통계 이벤트 본문")
                .authorId(authorId)
                .authorName("홈 통계 테스트")
                .publishedAt(NOW)
                .eventStartAt(startAt)
                .eventEndAt(endAt)
                .pinned(false)
                .build());
    }

    private void saveNotice(long authorId) {
        newsJpaRepository.saveAndFlush(NewsEntity.builder()
                .type(NewsType.NOTICE)
                .title("홈 통계 공지")
                .summary("홈 통계 공지 요약")
                .body("홈 통계 공지 본문")
                .authorId(authorId)
                .authorName("홈 통계 테스트")
                .publishedAt(NOW)
                .pinned(false)
                .build());
    }
}
