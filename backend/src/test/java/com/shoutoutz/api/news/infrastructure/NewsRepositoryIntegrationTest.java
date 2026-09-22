package com.shoutoutz.api.news.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsEventPeriod;
import com.shoutoutz.api.news.domain.NewsRepository;
import com.shoutoutz.api.news.domain.enums.NewsType;
import com.shoutoutz.api.news.infrastructure.jpa.NewsJpaRepository;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class NewsRepositoryIntegrationTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-09-05T00:00:00Z");

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private NewsJpaRepository newsJpaRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("소식 수정은 편집 가능 필드만 바꾸고 핀 상태와 불변 필드를 보존한다")
    void updatesOnlyEditableFields() {
        User user = userRepository.save(User.initialize("news-update-repository"));
        NewsEntity entity = newsJpaRepository.saveAndFlush(event(user.getId()));

        News current = newsRepository.findActiveById(entity.getId()).orElseThrow();
        News updated = current.update(
                "수정 제목",
                "수정 요약",
                "수정 본문",
                "수정 작성자",
                new NewsEventPeriod(
                        Instant.parse("2026-09-10T00:00:00Z"),
                        Instant.parse("2026-09-20T00:00:00Z")
                ),
                null
        );

        newsRepository.update(updated);
        newsJpaRepository.flush();

        NewsEntity saved = newsJpaRepository.findById(entity.getId()).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("수정 제목");
        assertThat(saved.getSummary()).isEqualTo("수정 요약");
        assertThat(saved.getBody()).isEqualTo("수정 본문");
        assertThat(saved.getAuthorName()).isEqualTo("수정 작성자");
        assertThat(saved.getCtaLabel()).isNull();
        assertThat(saved.getType()).isEqualTo(NewsType.EVENT);
        assertThat(saved.getAuthorId()).isEqualTo(user.getId());
        assertThat(saved.getPublishedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(saved.isPinned()).isTrue();
        assertThat(saved.getPinOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("소식을 원자적으로 소프트 삭제하고 활성 조회에서 제외한다")
    void softDeletesNews() {
        User user = userRepository.save(User.initialize("news-delete-repository"));
        NewsEntity entity = newsJpaRepository.saveAndFlush(notice(user.getId()));
        Instant deletedAt = Instant.parse("2026-09-19T12:00:00Z");

        assertThat(newsRepository.softDelete(entity.getId(), deletedAt)).isTrue();

        entityManager.clear();
        assertThat(newsRepository.findActiveById(entity.getId())).isEmpty();
        assertThat(newsJpaRepository.findById(entity.getId()).orElseThrow().getDeletedAt())
                .isEqualTo(deletedAt);
        assertThat(newsRepository.softDelete(entity.getId(), deletedAt)).isFalse();
    }

    private NewsEntity event(Long authorId) {
        return NewsEntity.builder()
                .type(NewsType.EVENT)
                .title("이벤트 제목")
                .summary("이벤트 요약")
                .body("이벤트 본문")
                .authorId(authorId)
                .authorName("작성자")
                .publishedAt(PUBLISHED_AT)
                .eventStartAt(PUBLISHED_AT.minusSeconds(60))
                .eventEndAt(PUBLISHED_AT.plusSeconds(60))
                .pinned(true)
                .pinOrder(1)
                .ctaLabel("참여하기")
                .ctaUrl("/events/1")
                .build();
    }

    private NewsEntity notice(Long authorId) {
        return NewsEntity.builder()
                .type(NewsType.NOTICE)
                .title("공지 제목")
                .summary("공지 요약")
                .body("공지 본문")
                .authorId(authorId)
                .authorName("작성자")
                .publishedAt(PUBLISHED_AT)
                .pinned(false)
                .pinOrder(null)
                .build();
    }
}
