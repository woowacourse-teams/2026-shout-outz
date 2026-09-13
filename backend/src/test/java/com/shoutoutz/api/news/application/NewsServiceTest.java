package com.shoutoutz.api.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.news.application.dto.NewsDetail;
import com.shoutoutz.api.news.application.dto.NewsPage;
import com.shoutoutz.api.news.application.dto.NewsSummary;
import com.shoutoutz.api.news.domain.enums.EventStatus;
import com.shoutoutz.api.news.domain.NewsErrorCode;
import com.shoutoutz.api.news.domain.NewsRepository;
import com.shoutoutz.api.news.domain.enums.NewsType;
import com.shoutoutz.api.news.presentation.dto.request.EventCreateRequest;
import com.shoutoutz.api.news.presentation.dto.request.NewsFindAllRequest;
import com.shoutoutz.api.news.presentation.dto.request.NewsFindRequest;
import com.shoutoutz.api.news.presentation.dto.request.NoticeCreateRequest;
import com.shoutoutz.api.news.presentation.dto.response.NewsFindAllResponse;
import com.shoutoutz.api.news.presentation.dto.response.NewsFindResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
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
        NoticeCreateRequest request = new NoticeCreateRequest(
                "제목",
                "요약",
                "본문",
                "작성자",
                new NoticeCreateRequest.Cta(" ", "example.com")
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

        NewsFindAllResponse response = newsService.findAll(
                new NewsFindAllRequest(null, null, "LATEST", 1, null)
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

        NewsFindAllResponse response = newsService.findAll(
                new NewsFindAllRequest("NOTICE", null, "LATEST", 20, null)
        );

        assertThat(response.items().get(0).eventStatus()).isNull();
        assertThat(response.items().get(0).eventStartAt()).isNull();
        assertThat(response.items().get(0).eventEndAt()).isNull();
        assertThat(response.meta().nextCursor()).isNull();
        assertThat(response.meta().hasNext()).isFalse();
    }

    @Test
    @DisplayName("소식 상세 조회 시 본문과 이전·다음 글을 반환한다")
    void findsNewsDetailWithNavigation() {
        NewsDetail detail = new NewsDetail(
                102L,
                NewsType.EVENT,
                "프로젝트 아카이빙 챌린지",
                "팀 프로젝트를 등록하고 동료 크루들의 피드백을 받아보세요.",
                1L,
                "샤라웃 운영팀",
                PUBLISHED_AT,
                PUBLISHED_AT.minusSeconds(60),
                PUBLISHED_AT.plusSeconds(60),
                false,
                null,
                new NewsDetail.Cta("프로젝트 등록하기", "/projects/3001"),
                new NewsDetail.Navigation(
                        101L,
                        "우아한테크코스 6기 최종 프로젝트 데모데이 안내",
                        PUBLISHED_AT.minusSeconds(120)
                ),
                new NewsDetail.Navigation(
                        103L,
                        "다음 소식",
                        PUBLISHED_AT.plusSeconds(120)
                )
        );
        when(newsQueryRepository.findDetailById(102L, true)).thenReturn(Optional.of(detail));
        when(clock.instant()).thenReturn(PUBLISHED_AT);

        NewsFindResponse response = newsService.findDetail(
                new NewsFindRequest(102L, true)
        );

        assertThat(response.id()).isEqualTo(102L);
        assertThat(response.type()).isEqualTo(NewsType.EVENT);
        assertThat(response.body()).isEqualTo(detail.body());
        assertThat(response.author().userId()).isEqualTo(1L);
        assertThat(response.author().name()).isEqualTo("샤라웃 운영팀");
        assertThat(response.eventStatus()).isEqualTo(EventStatus.ONGOING);
        assertThat(response.cta().label()).isEqualTo("프로젝트 등록하기");
        assertThat(response.previous().id()).isEqualTo(101L);
        assertThat(response.next().id()).isEqualTo(103L);
        verify(newsQueryRepository).findDetailById(102L, true);
        verify(clock).instant();
    }

    @Test
    @DisplayName("종료된 이벤트도 CTA를 반환한다")
    void returnsCtaWhenEventHasEnded() {
        NewsDetail detail = new NewsDetail(
                102L,
                NewsType.EVENT,
                "종료된 이벤트",
                "이벤트 본문",
                1L,
                "샤라웃 운영팀",
                PUBLISHED_AT,
                PUBLISHED_AT.minusSeconds(120),
                PUBLISHED_AT.minusSeconds(60),
                false,
                null,
                new NewsDetail.Cta("이벤트 확인", "/events/102"),
                null,
                null
        );
        when(newsQueryRepository.findDetailById(102L, false)).thenReturn(Optional.of(detail));
        when(clock.instant()).thenReturn(PUBLISHED_AT);

        NewsFindResponse response = newsService.findDetail(
                new NewsFindRequest(102L, false)
        );

        assertThat(response.eventStatus()).isEqualTo(EventStatus.ENDED);
        assertThat(response.cta()).isEqualTo(
                new NewsFindResponse.Cta("이벤트 확인", "/events/102")
        );
        assertThat(response.previous()).isNull();
        assertThat(response.next()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 소식이면 404 예외를 반환한다")
    void throwsNotFoundWhenNewsDoesNotExist() {
        when(newsQueryRepository.findDetailById(999L, true)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newsService.findDetail(
                new NewsFindRequest(999L, true)
        )).isInstanceOfSatisfying(EntityNotFoundException.class, error ->
                assertThat(error.getErrorCode()).isEqualTo(NewsErrorCode.NEWS_NOT_FOUND));

        verifyNoInteractions(clock);
    }

    @Test
    @DisplayName("소식 ID가 0 이하이면 요청 객체 생성 시 400 예외를 반환한다")
    void rejectsInvalidNewsIdAtRequestCreation() {
        assertThatThrownBy(() -> newsService.findDetail(
                new NewsFindRequest(0L, true)
        )).isInstanceOfSatisfying(InvalidInputException.class, error ->
                assertThat(error.getErrorCode()).isEqualTo(NewsErrorCode.NEWS_INVALID_ID_SIZE));

        verifyNoInteractions(clock, newsRepository, newsQueryRepository);
    }

    @Test
    @DisplayName("소식 조회 개수가 범위를 벗어나면 조회하지 않는다")
    void rejectsInvalidSize() {
        assertThatThrownBy(() -> newsService.findAll(
                new NewsFindAllRequest(null, null, "LATEST", 51, null)
        )).isInstanceOfSatisfying(BadRequestException.class, error ->
                assertThat(error.getErrorCode()).isEqualTo(NewsErrorCode.NEWS_INVALID_SIZE_FILTER_INPUT));

        verifyNoInteractions(clock, newsRepository, newsQueryRepository);
    }

    @Test
    @DisplayName("잘못된 커서는 조회하지 않는다")
    void rejectsInvalidCursor() {
        assertThatThrownBy(() -> newsService.findAll(
                new NewsFindAllRequest(null, null, "LATEST", 20, "invalid-cursor")
        )).isInstanceOfSatisfying(BadRequestException.class, error ->
                assertThat(error.getErrorCode()).isEqualTo(NewsErrorCode.NEWS_INVALID_CURSOR_FILTER_INPUT));

        verifyNoInteractions(clock, newsRepository, newsQueryRepository);
    }

    @Test
    @DisplayName("게시 시각이 없는 커서는 조회하지 않는다")
    void rejectsCursorWithoutPublishedAt() {
        String cursor = Base64.getEncoder().encodeToString(
                "{\"id\":102}".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> newsService.findAll(
                new NewsFindAllRequest(null, null, "LATEST", 20, cursor)
        )).isInstanceOfSatisfying(BadRequestException.class, error ->
                assertThat(error.getErrorCode()).isEqualTo(NewsErrorCode.NEWS_INVALID_CURSOR_FILTER_INPUT));

        verifyNoInteractions(clock, newsRepository, newsQueryRepository);
    }

    private void assertInvalidAuthorId(NoticeCreateRequest request) {
        assertThatThrownBy(() -> newsService.createNotice(request))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> Assertions.assertThat(error.getErrorCode())
                                .isEqualTo(NewsErrorCode.NEWS_INVALID_AUTHOR_ID_SIZE));
    }

    private NoticeCreateRequest requestWithCta() {
        return new NoticeCreateRequest(
                "데모데이 안내",
                "데모데이 일정을 안내합니다.",
                "2026년 9월 12일에 데모데이를 진행합니다.",
                "샤라웃 운영팀",
                new NoticeCreateRequest.Cta("일정 확인", "example.com")
        );
    }

    private NoticeCreateRequest requestWithoutCta() {
        return new NoticeCreateRequest(
                "서비스 점검 안내",
                "점검 일정을 안내합니다.",
                "2026년 9월 10일에 점검을 진행합니다.",
                "샤라웃 운영팀",
                null
        );
    }

    private EventCreateRequest eventRequestWithCta() {
        return new EventCreateRequest(
                "프로젝트 아카이빙 챌린지",
                "팀 프로젝트를 등록하고 피드백을 받아보세요.",
                "프로젝트를 등록하면 동료 크루들의 피드백을 받을 수 있습니다.",
                "샤라웃 운영팀",
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-30T23:59:59Z"),
                new EventCreateRequest.Cta("프로젝트 등록하기", "/projects/3001")
        );
    }

    // TODO: 인증 방식 확정 후 authorId 주입을 구현하면 저장 및 응답 성공 테스트를 추가한다.
}
