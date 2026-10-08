package com.shoutoutz.api.bugreport.application;

import static com.shoutoutz.api.bugreport.domain.BugReportErrorCode.BUG_REPORT_ADMIN_FORBIDDEN;
import static com.shoutoutz.api.bugreport.domain.BugReportErrorCode.BUG_REPORT_NOT_FOUND;

import com.shoutoutz.api.bugreport.application.dto.BugReportPage;
import com.shoutoutz.api.bugreport.application.dto.BugReportCursor;
import com.shoutoutz.api.bugreport.domain.BugReport;
import com.shoutoutz.api.bugreport.domain.BugReportRepository;
import com.shoutoutz.api.bugreport.domain.BugReportStatus;
import com.shoutoutz.api.bugreport.presentation.dto.request.BugReportAdminFindAllRequest;
import com.shoutoutz.api.bugreport.presentation.dto.request.BugReportStatusUpdateRequest;
import com.shoutoutz.api.bugreport.presentation.dto.response.BugReportAdminDetailResponse;
import com.shoutoutz.api.bugreport.presentation.dto.response.BugReportAdminFindAllResponse;
import com.shoutoutz.api.bugreport.presentation.dto.response.BugReportStatusUpdateResponse;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BugReportAdminService {

    private final BugReportAdminQueryRepository queryRepository;
    private final BugReportCursorCodec cursorCodec;
    private final BugReportRepository bugReportRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public BugReportAdminFindAllResponse findAll(
            UserRole role,
            BugReportAdminFindAllRequest request
    ) {
        validateAdmin(role);
        BugReportStatus status = request.resolvedStatus();
        BugReportPage page = queryRepository.findAll(
                status,
                cursorCodec.decode(request.cursor()),
                request.resolvedSize()
        );
        String nextCursor = page.hasNext()
                ? cursorCodec.encode(new BugReportCursor(
                        page.items().getLast().getCreatedAt(),
                        page.items().getLast().getId()
                ))
                : null;
        return BugReportAdminFindAllResponse.from(
                page.items(),
                new SliceMetaResponse(nextCursor, page.hasNext(), page.totalCount())
        );
    }

    @Transactional(readOnly = true)
    public BugReportAdminDetailResponse findDetail(long bugReportId, UserRole role) {
        validateAdmin(role);
        return BugReportAdminDetailResponse.from(findBugReport(bugReportId));
    }

    @Transactional
    public BugReportStatusUpdateResponse updateStatus(
            long bugReportId,
            long adminUserId,
            UserRole role,
            BugReportStatusUpdateRequest request
    ) {
        validateAdmin(role);
        BugReport bugReport = findBugReport(bugReportId);
        BugReport updated = bugReport.changeStatus(
                request.status(),
                adminUserId,
                clock.instant()
        );
        if (updated == bugReport) {
            return BugReportStatusUpdateResponse.from(bugReport);
        }
        return BugReportStatusUpdateResponse.from(bugReportRepository.save(updated));
    }

    private BugReport findBugReport(long bugReportId) {
        return bugReportRepository.findById(bugReportId)
                .orElseThrow(() -> new EntityNotFoundException(BUG_REPORT_NOT_FOUND));
    }

    private void validateAdmin(UserRole role) {
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException(BUG_REPORT_ADMIN_FORBIDDEN);
        }
    }
}
