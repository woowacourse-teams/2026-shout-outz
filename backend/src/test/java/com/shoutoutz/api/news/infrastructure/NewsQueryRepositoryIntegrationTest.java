package com.shoutoutz.api.news.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.news.application.NewsQueryRepository;
import com.shoutoutz.api.news.application.dto.NewsDetail;
import com.shoutoutz.api.news.domain.enums.NewsType;
import com.shoutoutz.api.news.infrastructure.jpa.NewsJpaRepository;
import com.shoutoutz.api.user.domain.User;
import com.shoutoutz.api.user.domain.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class NewsQueryRepositoryIntegrationTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-09-05T00:00:00Z");

    @Autowired
    private NewsQueryRepository newsQueryRepository;

    @Autowired
    private NewsJpaRepository newsJpaRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("상세 조회 시 엔티티를 도메인으로 변환하지 않고 NewsDetail projection으로 조회한다")
    void findsDetailWithProjection() {
        User user = userRepository.save(User.initialize("news-detail-projection"));
        NewsEntity entity = newsJpaRepository.saveAndFlush(event(user.getId(), PUBLISHED_AT));

        NewsDetail detail = newsQueryRepository.findDetailById(entity.getId(), false).orElseThrow();

        assertThat(detail.id()).isEqualTo(entity.getId());
        assertThat(detail.type()).isEqualTo(NewsType.EVENT);
        assertThat(detail.title()).isEqualTo("이벤트 제목");
        assertThat(detail.body()).isEqualTo("이벤트 본문");
        assertThat(detail.authorId()).isEqualTo(user.getId());
        assertThat(detail.authorName()).isEqualTo("작성자");
        assertThat(detail.publishedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(detail.eventStartAt()).isEqualTo(PUBLISHED_AT.minusSeconds(60));
        assertThat(detail.eventEndAt()).isEqualTo(PUBLISHED_AT.plusSeconds(60));
        assertThat(detail.cta()).isEqualTo(new NewsDetail.Cta("참여하기", "/events/1"));
        assertThat(detail.previous()).isNull();
        assertThat(detail.next()).isNull();
    }

    @Test
    @DisplayName("상세 조회 시 projection 결과에 이전·다음 소식을 조립한다")
    void findsDetailWithAdjacentNews() {
        User user = userRepository.save(User.initialize("news-detail-navigation"));
        NewsEntity previous = newsJpaRepository.save(event(
                user.getId(),
                PUBLISHED_AT.minusSeconds(60)
        ));
        NewsEntity current = newsJpaRepository.save(event(user.getId(), PUBLISHED_AT));
        NewsEntity next = newsJpaRepository.save(event(
                user.getId(),
                PUBLISHED_AT.plusSeconds(60)
        ));
        newsJpaRepository.flush();

        NewsDetail detail = newsQueryRepository.findDetailById(current.getId(), true).orElseThrow();

        assertThat(detail.previous().id()).isEqualTo(previous.getId());
        assertThat(detail.next().id()).isEqualTo(next.getId());
    }

    private NewsEntity event(Long authorId, Instant publishedAt) {
        return NewsEntity.builder()
                .type(NewsType.EVENT)
                .title("이벤트 제목")
                .summary("이벤트 요약")
                .body("이벤트 본문")
                .authorId(authorId)
                .authorName("작성자")
                .publishedAt(publishedAt)
                .eventStartAt(publishedAt.minusSeconds(60))
                .eventEndAt(publishedAt.plusSeconds(60))
                .pinned(false)
                .pinOrder(null)
                .ctaLabel("참여하기")
                .ctaUrl("/events/1")
                .build();
    }
}
