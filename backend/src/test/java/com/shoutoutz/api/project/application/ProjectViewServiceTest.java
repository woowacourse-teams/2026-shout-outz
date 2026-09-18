package com.shoutoutz.api.project.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.ProjectViewRepository;
import com.shoutoutz.api.project.presentation.dto.response.ProjectViewRecordResponse;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectViewServiceTest {

    private static final long PROJECT_ID = 1L;
    private static final VisitorKey VISITOR = new VisitorKey("a".repeat(64));

    @Mock
    private ProjectViewRepository projectViewRepository;

    @Test
    @DisplayName("조회할 수 있는 프로젝트면 현재 시각과 한국 날짜로 조회를 기록하고, 기록 후 조회수를 반환한다")
    void recordsView() {
        Instant now = Instant.parse("2026-09-18T01:00:00Z");
        given(projectViewRepository.existsViewableProject(PROJECT_ID)).willReturn(true);
        given(projectViewRepository.record(PROJECT_ID, VISITOR, LocalDate.parse("2026-09-18"), now))
                .willReturn(129L);

        ProjectViewRecordResponse response = service(now).record(PROJECT_ID, VISITOR);

        assertThat(response).isEqualTo(new ProjectViewRecordResponse(129));
    }

    @ParameterizedTest
    @CsvSource({
            "2026-09-18T14:59:59Z, 2026-09-18",
            "2026-09-18T15:00:00Z, 2026-09-19",
            "2026-09-17T15:00:00Z, 2026-09-18"
    })
    @DisplayName("조회 날짜는 UTC 가 아니라 한국 시간 자정을 기준으로 나뉜다")
    void usesKoreanDate(String nowText, String expectedDate) {
        Instant now = Instant.parse(nowText);
        given(projectViewRepository.existsViewableProject(PROJECT_ID)).willReturn(true);

        service(now).record(PROJECT_ID, VISITOR);

        verify(projectViewRepository).record(PROJECT_ID, VISITOR, LocalDate.parse(expectedDate), now);
    }

    @Test
    @DisplayName("같은 날 이미 기록된 조회여도 예외 없이 현재 조회수를 반환한다")
    void returnsCurrentViewCountForAlreadyRecordedView() {
        Instant now = Instant.parse("2026-09-18T01:00:00Z");
        given(projectViewRepository.existsViewableProject(PROJECT_ID)).willReturn(true);
        given(projectViewRepository.record(PROJECT_ID, VISITOR, LocalDate.parse("2026-09-18"), now))
                .willReturn(128L);

        ProjectViewRecordResponse response = service(now).record(PROJECT_ID, VISITOR);

        assertThat(response.viewCount()).isEqualTo(128);
    }

    @Test
    @DisplayName("없거나 삭제됐거나 승인되지 않은 프로젝트면 찾을 수 없다고 응답하고 기록하지 않는다")
    void rejectsNotViewableProject() {
        given(projectViewRepository.existsViewableProject(PROJECT_ID)).willReturn(false);
        ProjectViewService service = service(Instant.parse("2026-09-18T01:00:00Z"));

        assertThatThrownBy(() -> service.record(PROJECT_ID, VISITOR))
                .isInstanceOfSatisfying(EntityNotFoundException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
        verify(projectViewRepository, never()).record(anyLong(), any(), any(), any());
    }

    private ProjectViewService service(Instant now) {
        return new ProjectViewService(projectViewRepository, Clock.fixed(now, ZoneOffset.UTC));
    }
}
