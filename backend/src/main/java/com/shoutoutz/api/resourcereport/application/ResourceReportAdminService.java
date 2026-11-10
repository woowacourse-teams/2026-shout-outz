package com.shoutoutz.api.resourcereport.application;

import static com.shoutoutz.api.resourcereport.domain.ResourceReportErrorCode.RESOURCE_REPORT_ADMIN_FORBIDDEN;
import static com.shoutoutz.api.resourcereport.domain.ResourceReportErrorCode.RESOURCE_REPORT_NOT_FOUND;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.resourcereport.application.dto.ResourceReportAdminCursor;
import com.shoutoutz.api.resourcereport.application.dto.ResourceReportAdminItem;
import com.shoutoutz.api.resourcereport.application.dto.ResourceReportAdminPage;
import com.shoutoutz.api.resourcereport.domain.ResourceReport;
import com.shoutoutz.api.resourcereport.domain.ResourceReportRepository;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceReportAdminFindAllRequest;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceReportStatusUpdateRequest;
import com.shoutoutz.api.resourcereport.presentation.dto.response.ResourceReportAdminFindAllResponse;
import com.shoutoutz.api.resourcereport.presentation.dto.response.ResourceReportStatusUpdateResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영자가 받은 자료 제보를 보고 처리한다. 버그 제보 관리자 기능과 같은 방식이다.
 */
@Service
@RequiredArgsConstructor
public class ResourceReportAdminService {

    private final ResourceReportAdminQueryRepository resourceReportAdminQueryRepository;
    private final ResourceReportRepository resourceReportRepository;
    private final ResourceReportAdminCursorCodec cursorCodec;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ResourceReportAdminFindAllResponse findAll(
            UserRole role,
            ResourceReportAdminFindAllRequest request
    ) {
        validateAdmin(role);
        ResourceReportAdminPage page = resourceReportAdminQueryRepository.findAll(
                request.resolvedStatus(),
                request.resolvedKind(),
                cursorCodec.decode(request.cursor()),
                request.resolvedSize()
        );
        String nextCursor = page.hasNext() ? nextCursor(page) : null;
        return ResourceReportAdminFindAllResponse.from(
                page.items(),
                new SliceMetaResponse(nextCursor, page.hasNext(), page.totalCount())
        );
    }

    @Transactional
    public ResourceReportStatusUpdateResponse updateStatus(
            long reportId,
            long adminUserId,
            UserRole role,
            ResourceReportStatusUpdateRequest request
    ) {
        validateAdmin(role);
        ResourceReport report = resourceReportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException(RESOURCE_REPORT_NOT_FOUND));
        ResourceReport updated = report.changeStatus(request.status(), adminUserId, clock.instant());
        if (updated == report) {
            return ResourceReportStatusUpdateResponse.from(report);
        }
        return ResourceReportStatusUpdateResponse.from(resourceReportRepository.updateStatus(updated));
    }

    private String nextCursor(ResourceReportAdminPage page) {
        ResourceReportAdminItem last = page.items().getLast();
        return cursorCodec.encode(new ResourceReportAdminCursor(last.createdAt(), last.reportId()));
    }

    private void validateAdmin(UserRole role) {
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException(RESOURCE_REPORT_ADMIN_FORBIDDEN);
        }
    }
}
