package com.shoutoutz.api.jobposting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListCursor;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListItem;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListPage;
import com.shoutoutz.api.jobposting.application.dto.JobPostingSearchCondition;
import com.shoutoutz.api.jobposting.domain.JobPostingErrorCode;
import com.shoutoutz.api.jobposting.domain.JobPostingSort;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.presentation.dto.request.JobPostingFindAllRequest;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingFindAllResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
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

    @Mock
    private JobPostingQueryRepository queryRepository;

    private JobPostingService service;

    @BeforeEach
    void setUp() {
        service = new JobPostingService(queryRepository, Clock.fixed(NOW, ZoneOffset.UTC));
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
