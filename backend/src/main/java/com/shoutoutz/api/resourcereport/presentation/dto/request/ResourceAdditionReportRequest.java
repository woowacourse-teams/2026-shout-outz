package com.shoutoutz.api.resourcereport.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.CodePointLength;

/**
 * 빠진 자료 제보 요청. 이 프로젝트 리포지토리 아래의 PR이나 문서 주소를 받는다.
 */
public record ResourceAdditionReportRequest(
        @NotBlank(message = "url은 필수입니다.")
        @CodePointLength(max = 500, message = "url은 500자를 초과할 수 없습니다.")
        String url,

        @CodePointLength(max = 500, message = "memo는 500자를 초과할 수 없습니다.")
        String memo
) {

    public ResourceAdditionReportRequest {
        url = url == null ? null : url.trim();
        memo = memo == null || memo.isBlank() ? null : memo.trim();
    }
}
