package com.shoutoutz.api.news.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import com.shoutoutz.api.news.domain.NewsErrorCode;
import com.shoutoutz.api.news.domain.NewsRepository;
import com.shoutoutz.api.news.presentation.dto.request.NoticeCreateRequest;
import java.time.Clock;
import java.time.Instant;
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
    private Clock clock;

    private NewsService newsService;

    @BeforeEach
    void setUp() {
        newsService = new NewsService(newsRepository, clock);
    }

    @Test
    @DisplayName("인증 구현 전 임시 작성자 ID가 유효하지 않아 CTA 포함 공지를 저장하지 않는다")
    void rejectsNoticeWithCtaWhileAuthenticationIsPending() {
        when(clock.instant()).thenReturn(PUBLISHED_AT);

        assertInvalidAuthorId(requestWithCta());

        verify(clock).instant();
        verifyNoInteractions(newsRepository);
    }

    @Test
    @DisplayName("인증 구현 전 임시 작성자 ID가 유효하지 않아 CTA 없는 공지도 저장하지 않는다")
    void rejectsNoticeWithoutCtaWhileAuthenticationIsPending() {
        when(clock.instant()).thenReturn(PUBLISHED_AT);

        assertInvalidAuthorId(requestWithoutCta());

        verify(clock).instant();
        verifyNoInteractions(newsRepository);
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

        verifyNoInteractions(clock, newsRepository);
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

    // TODO: 인증 방식 확정 후 authorId 주입을 구현하면 저장 및 응답 성공 테스트를 추가한다.
}
