package com.shoutoutz.api.resourcereport.application.dto;

import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import java.time.Instant;

/**
 * 내가 보낸 자료 제보 한 건
 *
 * @param resourceId 필요 없어요 제보가 가리키는 자료 id. 추가해 주세요 제보이거나 자료가 내려갔으면 null
 * @param url        추가해 주세요 제보로 알려 준 주소. 필요 없어요 제보이면 null
 */
public record MyResourceReport(
        long reportId,
        ResourceReportKind kind,
        Long resourceId,
        String url,
        ResourceReportStatus status,
        Instant createdAt
) {
}
