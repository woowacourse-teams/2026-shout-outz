package com.shoutoutz.api.jobposting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListCursor;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListItem;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListPage;
import com.shoutoutz.api.jobposting.application.dto.JobPostingSearchCondition;
import com.shoutoutz.api.jobposting.application.dto.RecommendedResource;
import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.JobPostingContentHash;
import com.shoutoutz.api.jobposting.domain.JobPostingErrorCode;
import com.shoutoutz.api.jobposting.domain.JobPostingRepository;
import com.shoutoutz.api.jobposting.domain.JobPostingSort;
import com.shoutoutz.api.jobposting.domain.JobRequirement;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingStatus;
import com.shoutoutz.api.jobposting.domain.RecommendationVerdict;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import com.shoutoutz.api.jobposting.presentation.dto.request.JobPostingFindAllRequest;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingDetailResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingFindAllResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobRequirementRecommendationResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobPostingServiceTest {

    /**
     * UTC 로는 11월 4일이지만 한국 시간으로는 11월 5일 00시 30분이다.
     */
    private static final Instant NOW = Instant.parse("2026-11-04T15:30:00Z");
    private static final LocalDate KOREA_TODAY = LocalDate.of(2026, 11, 5);
    private static final Instant CREATED_AT = Instant.parse("2026-11-01T01:00:00Z");
    private static final long JOB_POSTING_ID = 100L;
    private static final String SOURCE_URL = "https://careers.example.com/jobs/100";

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Mock
    private JobPostingQueryRepository queryRepository;

    private JobPostingService service;

    @BeforeEach
    void setUp() {
        service = new JobPostingService(jobPostingRepository, queryRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("오늘 날짜는 한국 시간으로 구해 목록 조회와 직군별 개수에 넘긴다.")
    void usesKoreaToday() {
        givenPage(new JobPostingListPage(List.of(), false));

        service.findAll(new JobPostingFindAllRequest(JobRole.BACKEND, "토스", null, null, 10));

        JobPostingSearchCondition condition = capturedCondition();
        assertThat(condition).isEqualTo(new JobPostingSearchCondition(JobRole.BACKEND, "토스", KOREA_TODAY, null, 10));
        verify(queryRepository).countByJobRole("토스", KOREA_TODAY);
    }

    @Test
    @DisplayName("커서가 있으면 풀어서 조회 조건에 넣는다.")
    void decodesCursor() {
        JobPostingListCursor cursor = JobPostingListCursor.latest(CREATED_AT, 7L);
        givenPage(new JobPostingListPage(List.of(), false));

        service.findAll(new JobPostingFindAllRequest(null, null, null, JobPostingListCursorCodec.encode(cursor), null));

        assertThat(capturedCondition().cursor()).isEqualTo(cursor);
    }

    @Test
    @DisplayName("커서를 풀 수 없으면 400이고 공고를 읽지 않는다.")
    void rejectsInvalidCursor() {
        JobPostingFindAllRequest request = new JobPostingFindAllRequest(null, null, JobPostingSort.LATEST, "broken", null);

        assertThatThrownBy(() -> service.findAll(request))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_CURSOR_INVALID);
        verifyNoInteractions(queryRepository);
    }

    @Test
    @DisplayName("다음 페이지가 있으면 마지막 공고로 다음 커서를 만든다.")
    void encodesNextCursorFromLastItem() {
        givenPage(new JobPostingListPage(List.of(item(9L, CREATED_AT.plusSeconds(1)), item(8L, CREATED_AT)), true));

        JobPostingFindAllResponse response = service.findAll(new JobPostingFindAllRequest(null, null, null, null, 2));

        assertThat(response.meta().hasNext()).isTrue();
        assertThat(JobPostingListCursorCodec.decode(response.meta().nextCursor(), JobPostingSort.LATEST))
                .isEqualTo(JobPostingListCursor.latest(CREATED_AT, 8L));
    }

    @Test
    @DisplayName("다음 페이지가 없으면 다음 커서는 null 이다.")
    void returnsNullNextCursorOnLastPage() {
        givenPage(new JobPostingListPage(List.of(item(8L, CREATED_AT)), false));

        JobPostingFindAllResponse response = service.findAll(new JobPostingFindAllRequest(null, null, null, null, 2));

        assertThat(response.meta().hasNext()).isFalse();
        assertThat(response.meta().nextCursor()).isNull();
    }

    @Test
    @DisplayName("공고 목록과 직군별 개수를 응답에 담고, totalCount 는 고른 직군의 개수다.")
    void buildsResponse() {
        givenPage(new JobPostingListPage(List.of(item(8L, CREATED_AT)), false));
        when(queryRepository.countByJobRole(any(), any())).thenReturn(Map.of(JobRole.BACKEND, 1L, JobRole.FRONTEND, 4L));

        JobPostingFindAllResponse response = service.findAll(
                new JobPostingFindAllRequest(JobRole.BACKEND, null, null, null, null));

        assertThat(response.items()).containsExactly(new JobPostingFindAllResponse.Item(
                8L, "토스뱅크", JobRole.BACKEND, "정규직", "3년 이상", "Server Developer", null,
                4, List.of("Kotlin", "Spring", "Redis")));
        assertThat(response.meta().totalCount()).isEqualTo(1L);
        assertThat(response.meta().allCount()).isEqualTo(5L);
        assertThat(response.meta().jobRoleCounts()).hasSize(JobRole.values().length);
    }

    @Test
    @DisplayName("상세는 공고 정보와 역량을 순서대로 담고, 추천 자료가 없는 역량의 자료 수는 0이다.")
    void findsDetail() {
        given(posting(MatchingStatus.READY, null, null));
        when(queryRepository.countRecommendationsByRequirement(JOB_POSTING_ID)).thenReturn(Map.of(11L, 3L));

        JobPostingDetailResponse response = service.findDetail(JOB_POSTING_ID);

        assertThat(response).isEqualTo(new JobPostingDetailResponse(
                JOB_POSTING_ID, "토스뱅크", JobRole.BACKEND, "정규직", "3년 이상", "Server Developer", null,
                SOURCE_URL,
                List.of(
                        new JobPostingDetailResponse.Requirement(
                                11L, 1, "Kotlin, Spring", RequirementCategory.REQUIRED, "Spring 서버 개발 경험", 3L),
                        new JobPostingDetailResponse.Requirement(
                                12L, 2, "Kafka", RequirementCategory.PREFERRED, "메시지 큐 운영 경험", 0L))));
    }

    @Test
    @DisplayName("마감일이 한국 날짜로 오늘이면 상세를 보여 준다.")
    void findsDetailOnKoreaDeadline() {
        given(posting(MatchingStatus.READY, null, KOREA_TODAY));

        assertThat(service.findDetail(JOB_POSTING_ID).deadline()).isEqualTo(KOREA_TODAY);
    }

    @Test
    @DisplayName("없는 공고는 404다.")
    void rejectsMissingDetail() {
        when(jobPostingRepository.findById(JOB_POSTING_ID)).thenReturn(Optional.empty());

        assertNotFound();
    }

    @Test
    @DisplayName("분석이 안 끝난 공고는 404이고 자료 수를 세지 않는다.")
    void rejectsNotReadyDetail() {
        assertInvisible(posting(MatchingStatus.PROCESSING, null, null));
    }

    @Test
    @DisplayName("숨긴 공고는 404이고 자료 수를 세지 않는다.")
    void rejectsHiddenDetail() {
        assertInvisible(posting(MatchingStatus.READY, NOW, null));
    }

    @Test
    @DisplayName("한국 날짜로 마감이 지난 공고는 404이고 자료 수를 세지 않는다.")
    void rejectsClosedDetail() {
        assertInvisible(posting(MatchingStatus.READY, null, KOREA_TODAY.minusDays(1)));
    }

    @Test
    @DisplayName("역량별 자료는 저장소가 준 순서대로 카드 칸을 담는다.")
    void findsRecommendations() {
        given(posting(MatchingStatus.READY, null, null));
        when(queryRepository.findRecommendations(11L)).thenReturn(List.of(
                new RecommendedResource(1L, RecommendationVerdict.MATCH, "github-pr", "스레드 분리", "https://github.com/pr/1",
                        "스레드풀을 나눈 설계를 볼 수 있어요.", CREATED_AT, "implementation", "아맞다", "크루"),
                new RecommendedResource(2L, RecommendationVerdict.PARTIAL, "youtube-tekotalk", "호돌의 Spring WebFlux",
                        "https://youtube.com/watch?v=1", "리액티브 웹의 개념을 배울 수 있어요.", null, null, null, "호돌")));

        List<JobRequirementRecommendationResponse> response = service.findRecommendations(JOB_POSTING_ID, 11L);

        assertThat(response).containsExactly(
                new JobRequirementRecommendationResponse(1L, RecommendationVerdict.MATCH, "github-pr", "스레드 분리",
                        "https://github.com/pr/1", "스레드풀을 나눈 설계를 볼 수 있어요.", CREATED_AT, "implementation",
                        "아맞다", "크루"),
                new JobRequirementRecommendationResponse(2L, RecommendationVerdict.PARTIAL, "youtube-tekotalk",
                        "호돌의 Spring WebFlux", "https://youtube.com/watch?v=1", "리액티브 웹의 개념을 배울 수 있어요.",
                        null, null, null, "호돌"));
    }

    @Test
    @DisplayName("공고의 역량이지만 추천 자료가 없으면 빈 목록이다.")
    void returnsEmptyRecommendations() {
        given(posting(MatchingStatus.READY, null, null));
        when(queryRepository.findRecommendations(12L)).thenReturn(List.of());

        assertThat(service.findRecommendations(JOB_POSTING_ID, 12L)).isEmpty();
    }

    @Test
    @DisplayName("볼 수 없는 공고의 역량별 자료는 공고 404이고 자료를 읽지 않는다.")
    void rejectsRecommendationsOfInvisiblePosting() {
        given(posting(MatchingStatus.READY, NOW, null));

        assertThatThrownBy(() -> service.findRecommendations(JOB_POSTING_ID, 11L))
                .isInstanceOf(EntityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_NOT_FOUND);
        verifyNoInteractions(queryRepository);
    }

    @Test
    @DisplayName("공고의 역량이 아니면 역량 404이고 자료를 읽지 않는다.")
    void rejectsRequirementOfOtherPosting() {
        given(posting(MatchingStatus.READY, null, null));

        assertThatThrownBy(() -> service.findRecommendations(JOB_POSTING_ID, 99L))
                .isInstanceOf(EntityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_REQUIREMENT_NOT_FOUND);
        verifyNoInteractions(queryRepository);
    }

    private void assertInvisible(JobPosting posting) {
        given(posting);

        assertNotFound();
        verifyNoInteractions(queryRepository);
    }

    private void given(JobPosting posting) {
        when(jobPostingRepository.findById(JOB_POSTING_ID)).thenReturn(Optional.of(posting));
    }

    private void assertNotFound() {
        assertThatThrownBy(() -> service.findDetail(JOB_POSTING_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_NOT_FOUND);
    }

    private JobPosting posting(MatchingStatus status, Instant hiddenAt, LocalDate deadline) {
        return JobPosting.reconstitute(
                JOB_POSTING_ID, SOURCE_URL, JobPostingContentHash.from("공고 원문").value(), "토스뱅크",
                JobRole.BACKEND, "3년 이상", "Server Developer", "정규직", deadline, null, status, hiddenAt, 1L,
                List.of(
                        JobRequirement.reconstitute(11L, 1, "Kotlin, Spring", RequirementCategory.REQUIRED,
                                "Spring 서버 개발 경험", MatchingStatus.READY),
                        JobRequirement.reconstitute(12L, 2, "Kafka", RequirementCategory.PREFERRED,
                                "메시지 큐 운영 경험", MatchingStatus.READY)),
                CREATED_AT, CREATED_AT);
    }

    private void givenPage(JobPostingListPage page) {
        when(queryRepository.findAll(any())).thenReturn(page);
    }

    private JobPostingSearchCondition capturedCondition() {
        ArgumentCaptor<JobPostingSearchCondition> captor = ArgumentCaptor.forClass(JobPostingSearchCondition.class);
        verify(queryRepository).findAll(captor.capture());
        return captor.getValue();
    }

    private JobPostingListItem item(long id, Instant createdAt) {
        return new JobPostingListItem(id, "토스뱅크", JobRole.BACKEND, "정규직", "3년 이상", "Server Developer", null,
                4, List.of("Kotlin", "Spring", "Redis"), createdAt);
    }
}
