package com.shoutoutz.api.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import com.shoutoutz.api.news.application.command.CreateEventCommand;
import com.shoutoutz.api.news.application.command.CreateEventResult;
import com.shoutoutz.api.news.application.command.CreateNoticeCommand;
import com.shoutoutz.api.news.application.command.CreateNoticeResult;
import com.shoutoutz.api.news.application.query.NewsCursorCodec;
import com.shoutoutz.api.news.application.query.NewsFindAllQuery;
import com.shoutoutz.api.news.application.query.NewsFindAllResult;
import com.shoutoutz.api.news.application.query.NewsPage;
import com.shoutoutz.api.news.application.query.NewsQueryErrorCode;
import com.shoutoutz.api.news.application.query.NewsQueryRepository;
import com.shoutoutz.api.news.application.query.NewsSummary;
import com.shoutoutz.api.news.domain.EventStatus;
import com.shoutoutz.api.news.domain.NewsErrorCode;
import com.shoutoutz.api.news.domain.NewsRepository;
import com.shoutoutz.api.news.domain.NewsType;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-09-05T00:00:00Z");

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NewsQueryRepository newsQueryRepository;

    @Mock
    private Clock clock;

    private NewsService newsService;

    @BeforeEach
    void setUp() {
        newsService = new NewsService(newsRepository, newsQueryRepository, clock);
    }

    @Test
    @DisplayName("인증 구현 전 임시 작성자 ID가 유효하지 않아 CTA 포함 공지를 저장하지 않는다")
    void rejectsNoticeWithCtaWhileAuthenticationIsPending() {
        when(clock.instant()).thenReturn(PUBLISHED_AT);

        assertInvalidAuthorId(requestWithCta());

        verify(clock).instant();
        verifyNoInteractions(newsRepository, newsQueryRepository);
    }

    @Test
    @DisplayName("인증 구현 전 임시 작성자 ID가 유효하지 않아 CTA 없는 공지도 저장하지 않는다")
    void rejectsNoticeWithoutCtaWhileAuthenticationIsPending() {
        when(clock.instant()).thenReturn(PUBLISHED_AT);

        assertInvalidAuthorId(requestWithoutCta());

        verify(clock).instant();
        verifyNoInteractions(newsRepository, newsQueryRepository);
    }

    @Test
    @DisplayName("CTA DTO의 라벨이 유효하지 않으면 작성자 ID 검증 전에 CTA 오류를 반환한다")
    void rejectsInvalidCtaBeforeAuthorValidation() {
        CreateNoticeCommand request = new CreateNoticeCommand(
                "제목",
                "요약",
                "본문",
                "작성자",
                new CreateNoticeCommand.Cta(" ", "example.com")
        );

        assertThatThrownBy(() -> newsService.createNotice(request))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> Assertions.assertThat(error.getErrorCode())
                                .isEqualTo(NewsErrorCode.NEWS_CTA_LABEL_NULL_OR_BLANK));

        verifyNoInteractions(clock, newsRepository, newsQueryRepository);
    }

    @Test
    @DisplayName("인증 구현 전 임시 작성자 ID가 유효하지 않아 이벤트를 저장하지 않는다")
    void rejectsEventWhileAuthenticationIsPending() {
        when(clock.instant()).thenReturn(PUBLISHED_AT);

        assertThatThrownBy(() -> newsService.createEvent(eventRequestWithCta()))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> Assertions.assertThat(error.getErrorCode())
                                .isEqualTo(NewsErrorCode.NEWS_INVALID_AUTHOR_ID_SIZE));

        verify(clock).instant();
        verifyNoInteractions(newsRepository, newsQueryRepository);
    }

    @Test
    @DisplayName("소식 목록을 조회하고 이벤트 상태와 다음 커서를 응답한다")
    void findsAllNewsWithNextCursor() {
        NewsSummary event = new NewsSummary(
                102L,
                NewsType.EVENT,
                "프로젝트 아카이빙 챌린지",
                "팀 프로젝트를 등록하고 피드백을 받아보세요.",
                PUBLISHED_AT,
                PUBLISHED_AT.minusSeconds(60),
                PUBLISHED_AT.plusSeconds(60),
                false,
                null
        );
        when(clock.instant()).thenReturn(PUBLISHED_AT);
        when(newsQueryRepository.findAll(null, null, PUBLISHED_AT, null, 1))
                .thenReturn(new NewsPage(List.of(event), true));

        NewsFindAllResult response = newsService.findAll(
                new NewsFindAllQuery(null, null, 1, null)
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).id()).isEqualTo(102L);
        assertThat(response.items().get(0).type()).isEqualTo(NewsType.EVENT);
        assertThat(response.items().get(0).eventStatus()).isEqualTo(EventStatus.ONGOING);
        assertThat(response.meta().hasNext()).isTrue();
        assertThat(NewsCursorCodec.decode(response.meta().nextCursor()).id()).isEqualTo(102L);
        assertThat(NewsCursorCodec.decode(response.meta().nextCursor()).publishedAt())
                .isEqualTo(PUBLISHED_AT);
        verify(newsQueryRepository).findAll(null, null, PUBLISHED_AT, null, 1);
    }

    @Test
    @DisplayName("공지 목록 조회 시 이벤트 상태를 null로 반환한다")
    void returnsNullEventFieldsForNotice() {
        NewsSummary notice = new NewsSummary(
                101L,
                NewsType.NOTICE,
                "데모데이 안내",
                "데모데이 일정을 안내합니다.",
                PUBLISHED_AT,
                null,
                null,
                false,
                null
        );
        when(clock.instant()).thenReturn(PUBLISHED_AT);
        when(newsQueryRepository.findAll(NewsType.NOTICE, null, PUBLISHED_AT, null, 20))
                .thenReturn(new NewsPage(List.of(notice), false));

        NewsFindAllResult response = newsService.findAll(
                new NewsFindAllQuery(NewsType.NOTICE, null, 20, null)
        );

        assertThat(response.items().get(0).eventStatus()).isNull();
        assertThat(response.items().get(0).eventStartAt()).isNull();
        assertThat(response.items().get(0).eventEndAt()).isNull();
        assertThat(response.meta().nextCursor()).isNull();
        assertThat(response.meta().hasNext()).isFalse();
    }

    @Test
    @DisplayName("소식 조회 개수가 범위를 벗어나면 조회하지 않는다")
    void rejectsInvalidSize() {
        assertThatThrownBy(() -> newsService.findAll(
                new NewsFindAllQuery(null, null, 51, null)
        )).isInstanceOfSatisfying(BadRequestException.class, error ->
                assertThat(error.getErrorCode()).isEqualTo(NewsQueryErrorCode.NEWS_INVALID_SIZE));

        verifyNoInteractions(clock, newsRepository, newsQueryRepository);
    }

    @Test
    @DisplayName("잘못된 커서는 조회하지 않는다")
    void rejectsInvalidCursor() {
        assertThatThrownBy(() -> newsService.findAll(
                new NewsFindAllQuery(null, null, 20, "invalid-cursor")
        )).isInstanceOfSatisfying(BadRequestException.class, error ->
                assertThat(error.getErrorCode()).isEqualTo(NewsQueryErrorCode.NEWS_INVALID_CURSOR));

        verifyNoInteractions(clock, newsRepository, newsQueryRepository);
    }

    @Test
    @DisplayName("게시 시각이 없는 커서는 조회하지 않는다")
    void rejectsCursorWithoutPublishedAt() {
        String cursor = Base64.getEncoder().encodeToString(
                "{\"id\":102}".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> newsService.findAll(
                new NewsFindAllQuery(null, null, 20, cursor)
        )).isInstanceOfSatisfying(BadRequestException.class, error ->
                assertThat(error.getErrorCode()).isEqualTo(NewsQueryErrorCode.NEWS_INVALID_CURSOR));

        verifyNoInteractions(clock, newsRepository, newsQueryRepository);
    }

    private void assertInvalidAuthorId(CreateNoticeCommand request) {
        assertThatThrownBy(() -> newsService.createNotice(request))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> Assertions.assertThat(error.getErrorCode())
                                .isEqualTo(NewsErrorCode.NEWS_INVALID_AUTHOR_ID_SIZE));
    }

    private CreateNoticeCommand requestWithCta() {
        return new CreateNoticeCommand(
                "데모데이 안내",
                "데모데이 일정을 안내합니다.",
                "2026년 9월 12일에 데모데이를 진행합니다.",
                "샤라웃 운영팀",
                new CreateNoticeCommand.Cta("일정 확인", "example.com")
        );
    }

    private CreateNoticeCommand requestWithoutCta() {
        return new CreateNoticeCommand(
                "서비스 점검 안내",
                "점검 일정을 안내합니다.",
                "2026년 9월 10일에 점검을 진행합니다.",
                "샤라웃 운영팀",
                null
        );
    }

    private CreateEventCommand eventRequestWithCta() {
        return new CreateEventCommand(
                "프로젝트 아카이빙 챌린지",
                "팀 프로젝트를 등록하고 피드백을 받아보세요.",
                "프로젝트를 등록하면 동료 크루들의 피드백을 받을 수 있습니다.",
                "샤라웃 운영팀",
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-30T23:59:59Z"),
                new CreateEventCommand.Cta("프로젝트 등록하기", "/projects/3001")
        );
    }

    // TODO: 인증 방식 확정 후 authorId 주입을 구현하면 저장 및 응답 성공 테스트를 추가한다.
}
