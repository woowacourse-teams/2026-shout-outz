package com.shoutoutz.api.resourcereport.presentation.dto.response;

/**
 * 빠진 자료 제보로 접수한 제보. 이미 보낸 주소면 그때의 제보 id 를 돌려준다.
 */
public record ResourceAdditionReportResponse(
        long reportId
) {
}
