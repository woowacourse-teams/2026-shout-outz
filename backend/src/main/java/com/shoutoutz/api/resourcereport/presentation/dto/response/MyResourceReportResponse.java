package com.shoutoutz.api.resourcereport.presentation.dto.response;

import com.shoutoutz.api.resourcereport.application.dto.MyResourceReport;
import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import java.time.Instant;

/**
 * 내가 보낸 자료 제보 한 건. 프론트는 필요 없어요 제보의 resourceId 로 '필요 없어요 보냄' 배지를 붙인다.
 */
public record MyResourceReportResponse(
        long reportId,
        ResourceReportKind kind,
        Long resourceId,
        String url,
        ResourceReportStatus status,
        Instant createdAt
) {

    public static MyResourceReportResponse from(MyResourceReport report) {
        return new MyResourceReportResponse(
                report.reportId(),
                report.kind(),
                report.resourceId(),
                report.url(),
                report.status(),
                report.createdAt()
        );
    }
}
