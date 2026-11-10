package com.shoutoutz.api.resourcereport.presentation.dto.response;

/**
 * 필요 없는 자료 제보로 접수한 제보 한 건. 이미 보낸 자료면 그때의 제보 id 를 돌려준다.
 */
public record ResourceRemovalReportResponse(
        long reportId,
        long resourceId
) {
}
