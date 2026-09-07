package com.shoutoutz.api.news.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NewsTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-09-05T00:00:00Z");
    private static final Instant EVENT_START_AT = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant EVENT_END_AT = Instant.parse("2026-09-30T23:59:59Z");

    @Test
    @DisplayName("공지 생성 시 공지 유형과 핀 기본값을 설정한다")
    void createsNoticeWithDefaultPinState() {
        News notice = News.createNotice(
                "  서비스 점검 안내  ",
                " 안정적인 서비스 제공을 위해 점검을 진행합니다. ",
                " 2026년 9월 10일 02시부터 점검을 진행합니다. ",
                1L,
                " 샤라웃 운영팀 ",
                null,
                PUBLISHED_AT
        );

        assertThat(notice.getId()).isNull();
        assertThat(notice.getType()).isEqualTo(NewsType.NOTICE);
        assertThat(notice.getTitle()).isEqualTo("서비스 점검 안내");
        assertThat(notice.getSummary()).isEqualTo("안정적인 서비스 제공을 위해 점검을 진행합니다.");
        assertThat(notice.getBody()).isEqualTo("2026년 9월 10일 02시부터 점검을 진행합니다.");
        assertThat(notice.getAuthorId()).isEqualTo(1L);
        assertThat(notice.getAuthorName()).isEqualTo("샤라웃 운영팀");
        assertThat(notice.getPublishedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(notice.isPinned()).isFalse();
        assertThat(notice.getPinOrder()).isNull();
        assertThat(notice.getCta()).isNull();
    }

    @Test
    @DisplayName("공지 생성 시 CTA를 선택적으로 저장한다")
    void createsNoticeWithCta() {
        NewsCta cta = new NewsCta("일정 확인", "example.com");

        News notice = News.createNotice(
                "데모데이 안내",
                "데모데이 일정을 안내합니다.",
                "자세한 일정은 아래 버튼에서 확인하세요.",
                1L,
                "샤라웃 운영팀",
                cta,
                PUBLISHED_AT
        );

        assertThat(notice.getCta()).isEqualTo(cta);
    }

    @Test
    @DisplayName("이벤트 생성 시 이벤트 유형과 기간을 저장하고 고정하지 않는다")
    void createsEventWithPeriodAndDefaultPinState() {
        News event = News.createEvent(
                "프로젝트 아카이빙 챌린지",
                "팀 프로젝트를 등록하고 피드백을 받아보세요.",
                "프로젝트를 등록하면 동료 크루들의 피드백을 받을 수 있습니다.",
                1L,
                "샤라웃 운영팀",
                EVENT_START_AT,
                EVENT_END_AT,
                null,
                PUBLISHED_AT
        );

        assertThat(event.getType()).isEqualTo(NewsType.EVENT);
        assertThat(event.getEventStartAt()).isEqualTo(EVENT_START_AT);
        assertThat(event.getEventEndAt()).isEqualTo(EVENT_END_AT);
        assertThat(event.isPinned()).isFalse();
        assertThat(event.getPinOrder()).isNull();
    }

    @ParameterizedTest
    @MethodSource("eventPeriodErrors")
    @DisplayName("이벤트 기간이 없거나 순서가 잘못되면 생성할 수 없다")
    void rejectsInvalidEventPeriod(Instant startAt, Instant endAt, NewsErrorCode expected) {
        assertThatThrownBy(() -> News.createEvent(
                "이벤트",
                "요약",
                "본문",
                1L,
                "작성자",
                startAt,
                endAt,
                null,
                PUBLISHED_AT
        )).isInstanceOfSatisfying(DomainValidationException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(expected));
    }

    static Stream<Arguments> eventPeriodErrors() {
        return Stream.of(
                Arguments.of(null, EVENT_END_AT, NewsErrorCode.NEWS_EVENT_START_AT_NULL),
                Arguments.of(EVENT_START_AT, null, NewsErrorCode.NEWS_EVENT_END_AT_NULL),
                Arguments.of(EVENT_END_AT, EVENT_START_AT, NewsErrorCode.NEWS_EVENT_PERIOD_INVALID)
        );
    }

    @Test
    @DisplayName("작성자 ID가 0이면 공지를 생성할 수 없다")
    void rejectsInvalidAuthorId() {
        assertThatThrownBy(() -> News.createNotice(
                "제목",
                "요약",
                "본문",
                0L,
                "샤라웃 운영팀",
                null,
                PUBLISHED_AT
        )).isInstanceOfSatisfying(DomainValidationException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(NewsErrorCode.NEWS_INVALID_AUTHOR_ID_SIZE));
    }

    private static News.NewsBuilder validBuilder() {
        return News.builder().id(1L).type(NewsType.NOTICE)
                .title("제목").summary("요약").body("본문")
                .authorId(1L).authorName("작성자").publishedAt(PUBLISHED_AT);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, 1, Long.MAX_VALUE})
    @DisplayName("소식 ID는 현재 규칙상 null과 0 이상의 값을 허용한다")
    void acceptsId(Long id) {
        assertThat(validBuilder().id(id).build().getId()).isEqualTo(id);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, Long.MIN_VALUE})
    @DisplayName("소식 ID가 음수이면 생성할 수 없다")
    void rejectsNegativeId(long id) {
        assertError(validBuilder().id(id), NewsErrorCode.NEWS_INVALID_ID_SIZE);
    }

    @ParameterizedTest
    @EnumSource(NewsType.class)
    @DisplayName("소식 생성 시 지정한 유형을 저장한다")
    void preservesType(NewsType type) {
        News.NewsBuilder builder = validBuilder().type(type);
        if (type == NewsType.EVENT) {
            builder.eventStartAt(EVENT_START_AT).eventEndAt(EVENT_END_AT);
        }
        assertThat(builder.build().getType()).isEqualTo(type);
    }

    @Test
    @DisplayName("소식 유형이 null이면 생성할 수 없다")
    void rejectsNullType() {
        assertError(validBuilder().type(null), NewsErrorCode.NEWS_TYPE_NULL);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    @DisplayName("작성자 ID가 null이거나 0 이하이면 생성할 수 없다")
    void rejectsAuthorId(Long id) {
        assertError(validBuilder().authorId(id), id == null
                ? NewsErrorCode.NEWS_AUTHOR_ID_NULL : NewsErrorCode.NEWS_INVALID_AUTHOR_ID_SIZE);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, Long.MAX_VALUE})
    @DisplayName("작성자 ID가 1 이상이면 해당 값을 저장한다")
    void acceptsAuthorId(long id) {
        assertThat(validBuilder().authorId(id).build().getAuthorId()).isEqualTo(id);
    }

    @Test
    @DisplayName("게시 시각이 null이면 생성할 수 없다")
    void rejectsNullPublishedAt() {
        assertError(validBuilder().publishedAt(null), NewsErrorCode.NEWS_PUBLISHED_AT_NULL);
    }

    @ParameterizedTest
    @MethodSource("publishedTimes")
    @DisplayName("게시 시각은 시간 범위 제한 없이 입력값을 저장한다")
    void preservesPublishedAt(Instant time) {
        assertThat(validBuilder().publishedAt(time).build().getPublishedAt()).isEqualTo(time);
    }

    static Stream<Instant> publishedTimes() {
        return Stream.of(Instant.MIN, Instant.EPOCH, PUBLISHED_AT, Instant.MAX);
    }

    @ParameterizedTest
    @MethodSource("pinCases")
    @DisplayName("고정 여부와 순서의 null·음수·0·양수 조합을 검증한다")
    void validatesPin(boolean pinned, Integer order, NewsErrorCode expected) {
        News.NewsBuilder builder = validBuilder().pinned(pinned).pinOrder(order);
        if (expected != null) {
            assertError(builder, expected);
        } else {
            News news = builder.build();
            assertThat(news.isPinned()).isEqualTo(pinned);
            assertThat(news.getPinOrder()).isEqualTo(order);
        }
    }

    static Stream<Arguments> pinCases() {
        return Stream.of(false, true).flatMap(pinned ->
                Stream.of(null, Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE)
                        .map(order -> Arguments.of(pinned, order,
                                !pinned && order != null ? NewsErrorCode.NEWS_PIN_ORDER_NOT_NULL
                                : pinned && (order == null || order < 1)
                                        ? NewsErrorCode.NEWS_INVALID_PIN_ORDER : null)));
    }

    @ParameterizedTest
    @MethodSource("invalidTexts")
    @DisplayName("필수 문자열이 null이거나 정제 후 빈값 또는 공백이면 생성할 수 없다")
    void rejectsMissingText(TextField field, String value) {
        assertError(field.set(validBuilder(), value), field.requiredError);
    }

    static Stream<Arguments> invalidTexts() {
        return Stream.of(TextField.values()).flatMap(field ->
                Stream.of(null, "", " ", "\t\r\n", "\u2003", " \u2003 ", "\u0000")
                        .map(value -> Arguments.of(field, value)));
    }

    @ParameterizedTest
    @MethodSource("textBoundaries")
    @DisplayName("각 문자열은 정제 후 코드 포인트 기준 최대 길이까지 허용한다")
    void validatesTextLength(TextField field, String unit, int length) {
        String value = unit.repeat(length);
        News.NewsBuilder builder = field.set(validBuilder(), " \t" + value + "\r\n ");
        if (length > field.maxLength) {
            assertError(builder, field.lengthError);
        } else {
            assertThat(field.get(builder.build())).isEqualTo(value);
        }
    }

    static Stream<Arguments> textBoundaries() {
        return Stream.of(TextField.values()).flatMap(field ->
                Stream.of("a", "가", "😀").flatMap(unit ->
                        Stream.of(1, field.maxLength - 1, field.maxLength, field.maxLength + 1)
                                .map(length -> Arguments.of(field, unit, length))));
    }

    @ParameterizedTest
    @EnumSource(TextField.class)
    @DisplayName("문자열 내부의 연속 공백과 줄바꿈 및 탭을 유지한다")
    void preservesInternalWhitespace(TextField field) {
        String value = "첫 줄  내용\n다음\t줄";
        assertThat(field.get(field.set(validBuilder(), " " + value + " ").build())).isEqualTo(value);
    }

    @ParameterizedTest
    @EnumSource(TextField.class)
    @DisplayName("News의 trim은 텍스트 양끝 유니코드 공백을 제거하지 않는다")
    void preservesUnicodeWhitespaceAroundText(TextField field) {
        String value = "\u2003내용\u2003";
        assertThat(field.get(field.set(validBuilder(), value).build())).isEqualTo(value);
    }

    @Test
    @DisplayName("빌더는 CTA 생략을 허용하고 전달된 CTA 객체를 그대로 저장한다")
    void preservesOptionalCtaOnBuilder() {
        assertThat(validBuilder().build().getCta()).isNull();
        NewsCta cta = new NewsCta("열기", "example.com");
        assertThat(validBuilder().cta(cta).build().getCta()).isSameAs(cta);
    }

    private static void assertError(News.NewsBuilder builder, NewsErrorCode expected) {
        assertThatThrownBy(builder::build).isInstanceOfSatisfying(DomainValidationException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(expected));
    }

    enum TextField {
        TITLE(100, NewsErrorCode.NEWS_TITLE_NULL_OR_BLANK, NewsErrorCode.NEWS_INVALID_TITLE_LENGTH),
        SUMMARY(200, NewsErrorCode.NEWS_SUMMARY_NULL_OR_BLANK, NewsErrorCode.NEWS_INVALID_SUMMARY_LENGTH),
        BODY(100_000, NewsErrorCode.NEWS_BODY_NULL_OR_BLANK, NewsErrorCode.NEWS_INVALID_BODY_LENGTH),
        AUTHOR_NAME(50, NewsErrorCode.NEWS_AUTHOR_NAME_NULL_OR_BLANK, NewsErrorCode.NEWS_INVALID_AUTHOR_NAME_LENGTH);

        final int maxLength;
        final NewsErrorCode requiredError;
        final NewsErrorCode lengthError;

        TextField(int maxLength, NewsErrorCode requiredError, NewsErrorCode lengthError) {
            this.maxLength = maxLength;
            this.requiredError = requiredError;
            this.lengthError = lengthError;
        }

        News.NewsBuilder set(News.NewsBuilder builder, String value) {
            return switch (this) {
                case TITLE -> builder.title(value);
                case SUMMARY -> builder.summary(value);
                case BODY -> builder.body(value);
                case AUTHOR_NAME -> builder.authorName(value);
            };
        }

        String get(News news) {
            return switch (this) {
                case TITLE -> news.getTitle();
                case SUMMARY -> news.getSummary();
                case BODY -> news.getBody();
                case AUTHOR_NAME -> news.getAuthorName();
            };
        }
    }
}
