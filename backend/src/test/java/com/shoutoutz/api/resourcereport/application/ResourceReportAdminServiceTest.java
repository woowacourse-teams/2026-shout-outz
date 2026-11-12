package com.shoutoutz.api.resourcereport.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.resourcereport.application.dto.ResourceReportAdminCursor;
import com.shoutoutz.api.resourcereport.application.dto.ResourceReportAdminItem;
import com.shoutoutz.api.resourcereport.application.dto.ResourceReportAdminPage;
import com.shoutoutz.api.resourcereport.domain.ResourceReport;
import com.shoutoutz.api.resourcereport.domain.ResourceReportErrorCode;
import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcereport.domain.ResourceReportRepository;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceReportAdminFindAllRequest;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceReportStatusUpdateRequest;
import com.shoutoutz.api.resourcereport.presentation.dto.response.ResourceReportAdminFindAllResponse;
import com.shoutoutz.api.resourcereport.presentation.dto.response.ResourceReportStatusUpdateResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ResourceReportAdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T01:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-11-10T02:00:00Z");
    private static final long REPORT_ID = 41L;
    private static final long ADMIN_ID = 1L;

    @Mock
    private ResourceReportAdminQueryRepository resourceReportAdminQueryRepository;

    @Mock
    private ResourceReportRepository resourceReportRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final ResourceReportAdminCursorCodec cursorCodec = new ResourceReportAdminCursorCodec();

    private ResourceReportAdminService resourceReportAdminService;

    @BeforeEach
    void setUp() {
        resourceReportAdminService = new ResourceReportAdminService(
                resourceReportAdminQueryRepository,
                resourceReportRepository,
                cursorCodec,
                Clock.fixed(NOW, ZoneOffset.UTC),
                eventPublisher
        );
    }

    @Test
    @DisplayName("상태를 생략하면 확인 전(OPEN) 제보만 읽고, 다음 페이지가 있으면 마지막 제보로 커서를 만든다")
    void findsOpenReportsWithNextCursor() {
        ResourceReportAdminItem item = adminItem(REPORT_ID, CREATED_AT);
        given(resourceReportAdminQueryRepository.findAll(ResourceReportStatus.OPEN, ResourceReportKind.REMOVE, null, 1))
                .willReturn(new ResourceReportAdminPage(List.of(item), true, 3L));

        ResourceReportAdminFindAllResponse response = resourceReportAdminService.findAll(
                UserRole.ADMIN,
                new ResourceReportAdminFindAllRequest(null, "REMOVE", 1, null)
        );

        assertThat(response.items()).extracting(ResourceReportAdminFindAllResponse.Item::reportId)
                .containsExactly(REPORT_ID);
        assertThat(response.meta().hasNext()).isTrue();
        assertThat(response.meta().totalCount()).isEqualTo(3L);
        assertThat(cursorCodec.decode(response.meta().nextCursor()))
                .isEqualTo(new ResourceReportAdminCursor(CREATED_AT, REPORT_ID));
    }

    @Test
    @DisplayName("ALL 이면 상태로 거르지 않고, 마지막 페이지에는 커서를 주지 않는다")
    void findsAllStatusesOnLastPage() {
        given(resourceReportAdminQueryRepository.findAll(null, null, null, 20))
                .willReturn(new ResourceReportAdminPage(List.of(adminItem(REPORT_ID, CREATED_AT)), false, 1L));

        ResourceReportAdminFindAllResponse response = resourceReportAdminService.findAll(
                UserRole.ADMIN,
                new ResourceReportAdminFindAllRequest("ALL", null, null, null)
        );

        assertThat(response.meta().hasNext()).isFalse();
        assertThat(response.meta().nextCursor()).isNull();
    }

    @Test
    @DisplayName("관리자가 아니면 목록을 볼 수 없다")
    void rejectsNonAdminList() {
        assertThatThrownBy(() -> resourceReportAdminService.findAll(
                UserRole.USER,
                new ResourceReportAdminFindAllRequest(null, null, null, null)
        ))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ResourceReportErrorCode.RESOURCE_REPORT_ADMIN_FORBIDDEN);
        verifyNoInteractions(resourceReportAdminQueryRepository);
    }

    @Test
    @DisplayName("잘못된 커서는 거부한다")
    void rejectsInvalidCursor() {
        assertThatThrownBy(() -> resourceReportAdminService.findAll(
                UserRole.ADMIN,
                new ResourceReportAdminFindAllRequest(null, null, null, "invalid-cursor")
        ))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ResourceReportErrorCode.RESOURCE_REPORT_ADMIN_CURSOR_INVALID);
    }

    @Test
    @DisplayName("상태가 바뀌면 처리한 관리자와 시각을 저장한다")
    void updatesStatus() {
        given(resourceReportRepository.findById(REPORT_ID)).willReturn(Optional.of(openReport()));
        given(resourceReportRepository.updateStatus(any(ResourceReport.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        ResourceReportStatusUpdateResponse response = resourceReportAdminService.updateStatus(
                REPORT_ID,
                ADMIN_ID,
                UserRole.ADMIN,
                new ResourceReportStatusUpdateRequest(ResourceReportStatus.ACCEPTED)
        );

        assertThat(response).isEqualTo(new ResourceReportStatusUpdateResponse(
                REPORT_ID,
                ResourceReportStatus.ACCEPTED,
                NOW,
                ADMIN_ID
        ));
    }

    @Test
    @DisplayName("상태가 바뀌면 제보 종류를 담아 상태 변경을 알린다")
    void publishesStatusChange() {
        given(resourceReportRepository.findById(REPORT_ID)).willReturn(Optional.of(openReport()));
        given(resourceReportRepository.updateStatus(any(ResourceReport.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        resourceReportAdminService.updateStatus(
                REPORT_ID,
                ADMIN_ID,
                UserRole.ADMIN,
                new ResourceReportStatusUpdateRequest(ResourceReportStatus.ACCEPTED)
        );

        verify(eventPublisher).publishEvent(new ResourceReportStatusChanged(ResourceReportKind.REMOVE));
    }

    @Test
    @DisplayName("같은 상태로 바꾸면 상태 변경을 알리지 않는다")
    void doesNotPublishWhenStatusIsSame() {
        given(resourceReportRepository.findById(REPORT_ID)).willReturn(Optional.of(openReport()));

        resourceReportAdminService.updateStatus(
                REPORT_ID,
                ADMIN_ID,
                UserRole.ADMIN,
                new ResourceReportStatusUpdateRequest(ResourceReportStatus.OPEN)
        );

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("같은 상태로 바꾸면 저장하지 않는다")
    void keepsSameStatus() {
        given(resourceReportRepository.findById(REPORT_ID)).willReturn(Optional.of(openReport()));

        ResourceReportStatusUpdateResponse response = resourceReportAdminService.updateStatus(
                REPORT_ID,
                ADMIN_ID,
                UserRole.ADMIN,
                new ResourceReportStatusUpdateRequest(ResourceReportStatus.OPEN)
        );

        assertThat(response.status()).isEqualTo(ResourceReportStatus.OPEN);
        assertThat(response.handledAt()).isNull();
        verify(resourceReportRepository, never()).updateStatus(any());
    }

    @Test
    @DisplayName("없는 제보는 처리할 수 없다")
    void rejectsMissingReport() {
        given(resourceReportRepository.findById(REPORT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> resourceReportAdminService.updateStatus(
                REPORT_ID,
                ADMIN_ID,
                UserRole.ADMIN,
                new ResourceReportStatusUpdateRequest(ResourceReportStatus.ACCEPTED)
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ResourceReportErrorCode.RESOURCE_REPORT_NOT_FOUND);
    }

    @Test
    @DisplayName("관리자가 아니면 제보를 처리할 수 없다")
    void rejectsNonAdminUpdate() {
        assertThatThrownBy(() -> resourceReportAdminService.updateStatus(
                REPORT_ID,
                ADMIN_ID,
                UserRole.USER,
                new ResourceReportStatusUpdateRequest(ResourceReportStatus.ACCEPTED)
        ))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(ResourceReportErrorCode.RESOURCE_REPORT_ADMIN_FORBIDDEN);
        verifyNoInteractions(resourceReportRepository);
    }

    private ResourceReport openReport() {
        return ResourceReport.reconstitute(
                REPORT_ID,
                10L,
                20L,
                ResourceReportKind.REMOVE,
                "github-pr",
                "woowacourse-teams/2026-shout-outz#512",
                null,
                null,
                ResourceReportStatus.OPEN,
                CREATED_AT,
                CREATED_AT,
                null,
                null
        );
    }

    private ResourceReportAdminItem adminItem(long reportId, Instant createdAt) {
        return new ResourceReportAdminItem(
                reportId,
                ResourceReportKind.REMOVE,
                "shout-outz",
                "샤라웃",
                3412L,
                "github-pr",
                "PR 제목",
                "https://github.com/woowacourse-teams/2026-shout-outz/pull/512",
                null,
                "단순 설정 변경이에요",
                20L,
                ResourceReportStatus.OPEN,
                createdAt
        );
    }
}
