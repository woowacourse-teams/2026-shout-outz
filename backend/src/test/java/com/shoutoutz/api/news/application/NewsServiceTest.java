package com.shoutoutz.api.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.news.application.dto.NewsDetail;
import com.shoutoutz.api.news.application.dto.NewsPage;
import com.shoutoutz.api.news.application.dto.NewsSummary;
import com.shoutoutz.api.news.domain.enums.EventStatus;
import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsCta;
import com.shoutoutz.api.news.domain.NewsErrorCode;
import com.shoutoutz.api.news.domain.NewsRepository;
import com.shoutoutz.api.news.domain.enums.NewsType;
import com.shoutoutz.api.news.presentation.dto.request.EventCreateRequest;
import com.shoutoutz.api.news.presentation.dto.request.NewsFindAllRequest;
import com.shoutoutz.api.news.presentation.dto.request.NewsFindRequest;
import com.shoutoutz.api.news.presentation.dto.request.NewsUpdateRequest;
import com.shoutoutz.api.news.presentation.dto.request.NoticeCreateRequest;
import com.shoutoutz.api.news.presentation.dto.response.NewsFindAllResponse;
import com.shoutoutz.api.news.presentation.dto.response.NewsFindResponse;
import com.shoutoutz.api.news.presentation.dto.response.NewsUpdateResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
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
    @DisplayName("관리자 작성자 ID가 유효하지 않아 CTA 포함 공지를 저장하지 않는다")
    void rejectsNoticeWithCtaWhenAuthorIdIsInvalid() {
        when(clock.instant()).thenReturn(PUBLISHED_AT);

        assertInvalidAuthorId(requestWithCta(), 0L);

        verify(clock).instant();
        verifyNoInteractions(newsRepository, newsQueryRepository);
    }

    @Test
    @DisplayName("관리자 작성자 ID가 유효하지 않아 CTA 없는 공지도 저장하지 않는다")
    void rejectsNoticeWithoutCtaWhenAuthorIdIsInvalid() {
        when(clock.instant()).thenReturn(PUBLISHED_AT);

        assertInvalidAuthorId(requestWithoutCta(), 0L);

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

        assertThatThrownBy(() -> newsService.createNotice(0L, UserRole.ADMIN, request))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> Assertions.assertThat(error.getErrorCode())
                                .isEqualTo(NewsErrorCode.NEWS_CTA_LABEL_NULL_OR_BLANK));

        verifyNoInteractions(clock, newsRepository, newsQueryRepository);
    }

    @Test
    @DisplayName("관리자 작성자 ID가 유효하지 않아 이벤트를 저장하지 않는다")
    void rejectsEventWhenAuthorIdIsInvalid() {
        when(clock.instant()).thenReturn(PUBLISHED_AT);

        assertThatThrownBy(() -> newsService.createEvent(0L, UserRole.ADMIN, eventRequestWithCta()))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> Assertions.assertThat(error.getErrorCode())
                                .isEqualTo(NewsErrorCode.NEWS_INVALID_AUTHOR_ID_SIZE));

        verify(clock).instant();
        verifyNoInteractions(newsRepository, newsQueryRepository);
    }

    @Test
    @DisplayName("이벤트 생성의 시작 시각이 종료 시각보다 늦으면 400 통합 오류 코드로 거절한다")
    void rejectsInvalidEventPeriodWithBadRequest() {
        EventCreateRequest request = new EventCreateRequest(
                "이벤트",
                "요약",
                "본문",
                "작성자",
                Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-09-30T23:59:59Z"),
                null
        );

        assertThatThrownBy(() -> newsService.createEvent(1L, UserRole.ADMIN, request))
                .isInstanceOfSatisfying(BadRequestException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(NewsErrorCode.NEWS_EVENT_PERIOD_INVALID));

        verifyNoInteractions(clock, newsRepository, newsQueryRepository);
    }

    @Test
    @DisplayName("관리자가 아니면 공지 생성을 거절한다")
    void rejectsNoticeCreationWhenRoleIsNotAdmin() {
        assertThatThrownBy(() -> newsService.createNotice(
                1L, UserRole.USER, requestWithoutCta()))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(NewsErrorCode.NEWS_ADMIN_FORBIDDEN));

        verifyNoInteractions(clock, newsRepository, newsQueryRepository);
    }

    @Test
    @DisplayName("관리자가 아니면 이벤트 생성을 거절한다")
    void rejectsEventCreationWhenRoleIsNotAdmin() {
        assertThatThrownBy(() -> newsService.createEvent(
                1L, UserRole.USER, eventRequestWithCta()))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(NewsErrorCode.NEWS_ADMIN_FORBIDDEN));

        verifyNoInteractions(clock, newsRepository, newsQueryRepository);
    }

    @Test
    @DisplayName("관리자는 소식의 편집 가능 필드만 수정하고 수정 응답을 받는다")
    void updatesEditableNewsFieldsAsAdmin() {
        News current = News.createEvent(
                "기존 제목",
                "기존 요약",
                "기존 본문",
                1L,
                "기존 작성자",
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-30T23:59:59Z"),
                new NewsCta("기존 CTA", "/old"),
                PUBLISHED_AT
        );
        current = News.builder()
                .id(102L)
                .type(current.getType())
                .title(current.getTitle())
                .summary(current.getSummary())
                .body(current.getBody())
                .authorId(current.getAuthorId())
                .authorName(current.getAuthorName())
                .publishedAt(current.getPublishedAt())
                .eventPeriod(new com.shoutoutz.api.news.domain.NewsEventPeriod(
                        current.getEventStartAt(), current.getEventEndAt()))
                .pinned(true)
                .pinOrder(1)
                .cta(current.getCta())
                .build();
        NewsUpdateRequest request = updateRequest(
                "수정 제목",
                "수정 요약",
                "수정 본문",
                "수정 작성자",
                "2026-09-10T00:00:00Z",
                "2026-09-20T00:00:00Z",
                null
        );
        News expected = current.update(
                request.title(),
                request.summary(),
                request.body(),
                request.authorName(),
                new com.shoutoutz.api.news.domain.NewsEventPeriod(
                        request.eventStartAt(), request.eventEndAt()),
                null
        );
        when(newsRepository.findActiveById(102L)).thenReturn(Optional.of(current));
        when(newsRepository.update(org.mockito.ArgumentMatchers.any(News.class))).thenReturn(expected);
        when(clock.instant()).thenReturn(PUBLISHED_AT);

        NewsUpdateResponse response = newsService.update(102L, UserRole.ADMIN, request);

        assertThat(response.id()).isEqualTo(102L);
        assertThat(response.title()).isEqualTo("수정 제목");
        assertThat(response.summary()).isEqualTo("수정 요약");
        assertThat(response.isPinned()).isTrue();
        assertThat(response.pinOrder()).isEqualTo(1);
        assertThat(response.cta()).isNull();
        verify(newsRepository).update(org.mockito.ArgumentMatchers.any(News.class));
    }

    @Test
    @DisplayName("관리자가 아닌 사용자는 소식 수정에 접근할 수 없다")
    void rejectsNewsUpdateForNonAdmin() {
        NewsUpdateRequest request = updateRequest(
                "제목", "요약", "본문", "작성자", null, null, null);

        assertThatThrownBy(() -> newsService.update(102L, UserRole.USER, request))
                .isInstanceOfSatisfying(com.shoutoutz.api.common.exception.custom.ForbiddenException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(NewsErrorCode.NEWS_ADMIN_FORBIDDEN));

        verifyNoInteractions(newsRepository, newsQueryRepository, clock);
    }

    @Test
    @DisplayName("관리자가 소식을 소프트 삭제하고 삭제 시각을 반환한다")
    void softDeletesNews() {
        Instant deletedAt = Instant.parse("2026-09-19T12:00:00Z");
        when(clock.instant()).thenReturn(deletedAt);
        when(newsRepository.softDelete(102L, deletedAt)).thenReturn(true);

        var response = newsService.delete(102L, UserRole.ADMIN);

        assertThat(response.id()).isEqualTo(102L);
        assertThat(response.deletedAt()).isEqualTo(deletedAt);
        verify(newsRepository).softDelete(102L, deletedAt);
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

    private void assertInvalidAuthorId(NoticeCreateRequest request, long authorId) {
        assertThatThrownBy(() -> newsService.createNotice(authorId, UserRole.ADMIN, request))
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

    private NewsUpdateRequest updateRequest(
            String title,
            String summary,
            String body,
            String authorName,
            String eventStartAt,
            String eventEndAt,
            NewsUpdateRequest.Cta cta
    ) {
        NewsUpdateRequest request = new NewsUpdateRequest();
        request.setTitle(title);
        request.setSummary(summary);
        request.setBody(body);
        request.setAuthorName(authorName);
        request.setEventStartAt(eventStartAt == null ? null : Instant.parse(eventStartAt));
        request.setEventEndAt(eventEndAt == null ? null : Instant.parse(eventEndAt));
        request.setCta(cta);
        return request;
    }

    // TODO: 인증 방식 확정 후 authorId 주입을 구현하면 저장 및 응답 성공 테스트를 추가한다.
}
