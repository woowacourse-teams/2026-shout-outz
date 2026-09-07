package com.shoutoutz.api.news.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsCta;
import com.shoutoutz.api.news.domain.NewsType;
import com.shoutoutz.api.news.infrastructure.NewsEntity;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NewsMapperTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-09-05T00:00:00Z");

    @Test
    @DisplayName("공지의 필드를 엔티티와 도메인 사이에 변환한다")
    void 공지의_필드를_엔티티와_도메인_사이에_변환한다() {
        News notice = News.builder()
                .id(106L)
                .type(NewsType.NOTICE)
                .title("데모데이 안내")
                .summary("데모데이 일정을 안내합니다.")
                .body("자세한 일정은 아래 버튼에서 확인하세요.")
                .authorId(1L)
                .authorName("샤라웃 운영팀")
                .publishedAt(PUBLISHED_AT)
                .pinned(false)
                .pinOrder(null)
                .cta(new NewsCta("일정 확인", "example.com"))
                .build();

        NewsEntity entity = NewsMapper.toEntity(notice);

        assertThat(entity.getId()).isEqualTo(106L);
        assertThat(entity.getType()).isEqualTo(NewsType.NOTICE);
        assertThat(entity.getTitle()).isEqualTo(notice.getTitle());
        assertThat(entity.getSummary()).isEqualTo(notice.getSummary());
        assertThat(entity.getBody()).isEqualTo(notice.getBody());
        assertThat(entity.getAuthorId()).isEqualTo(1L);
        assertThat(entity.getAuthorName()).isEqualTo("샤라웃 운영팀");
        assertThat(entity.getPublishedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(entity.isPinned()).isFalse();
        assertThat(entity.getPinOrder()).isNull();
        assertThat(entity.getCtaLabel()).isEqualTo("일정 확인");
        assertThat(entity.getCtaUrl()).isEqualTo("example.com");

        News restored = NewsMapper.toDomain(entity);

        assertThat(restored.getId()).isEqualTo(notice.getId());
        assertThat(restored.getType()).isEqualTo(notice.getType());
        assertThat(restored.getTitle()).isEqualTo(notice.getTitle());
        assertThat(restored.getSummary()).isEqualTo(notice.getSummary());
        assertThat(restored.getBody()).isEqualTo(notice.getBody());
        assertThat(restored.getAuthorId()).isEqualTo(notice.getAuthorId());
        assertThat(restored.getAuthorName()).isEqualTo(notice.getAuthorName());
        assertThat(restored.getPublishedAt()).isEqualTo(notice.getPublishedAt());
        assertThat(restored.isPinned()).isEqualTo(notice.isPinned());
        assertThat(restored.getPinOrder()).isEqualTo(notice.getPinOrder());
        assertThat(restored.getCta()).isEqualTo(notice.getCta());
    }

    @Test
    @DisplayName("CTA가 없으면 엔티티와 도메인에 null로 변환한다")
    void cta가_없으면_엔티티와_도메인에_null로_변환한다() {
        News notice = News.createNotice(
                "서비스 점검 안내",
                "점검 일정을 안내합니다.",
                "점검은 새벽에 진행됩니다.",
                1L,
                "샤라웃 운영팀",
                null,
                PUBLISHED_AT
        );

        NewsEntity entity = NewsMapper.toEntity(notice);

        assertThat(entity.getCtaLabel()).isNull();
        assertThat(entity.getCtaUrl()).isNull();
        assertThat(NewsMapper.toDomain(entity).getCta()).isNull();
    }

    @Test
    @DisplayName("이벤트 기간을 엔티티와 도메인 사이에 변환한다")
    void mapsEventPeriodBetweenEntityAndDomain() {
        Instant startAt = Instant.parse("2026-09-01T00:00:00Z");
        Instant endAt = Instant.parse("2026-09-30T23:59:59Z");
        News event = News.createEvent(
                "이벤트",
                "요약",
                "본문",
                1L,
                "작성자",
                startAt,
                endAt,
                new NewsCta("열기", "/events/1"),
                PUBLISHED_AT
        );

        NewsEntity entity = NewsMapper.toEntity(event);

        assertThat(entity.getType()).isEqualTo(NewsType.EVENT);
        assertThat(entity.getEventStartAt()).isEqualTo(startAt);
        assertThat(entity.getEventEndAt()).isEqualTo(endAt);

        News restored = NewsMapper.toDomain(entity);

        assertThat(restored.getType()).isEqualTo(NewsType.EVENT);
        assertThat(restored.getEventStartAt()).isEqualTo(startAt);
        assertThat(restored.getEventEndAt()).isEqualTo(endAt);
        assertThat(restored.getCta()).isEqualTo(event.getCta());
    }
}
