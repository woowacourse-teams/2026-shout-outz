package com.shoutoutz.api.resourcereport.presentation.dto.request;

import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * 관리자 자료 제보 목록 조회 조건. 상태를 생략하면 아직 확인하지 않은 OPEN 만 읽는다(버그 제보 관리자 목록과 같음).
 */
public record ResourceReportAdminFindAllRequest(
        @Pattern(
                regexp = "OPEN|ACCEPTED|REJECTED|ALL",
                message = "status는 OPEN, ACCEPTED, REJECTED, ALL 중 하나여야 합니다."
        )
        String status,

        @Pattern(regexp = "REMOVE|ADD", message = "kind는 REMOVE, ADD 중 하나여야 합니다.")
        String kind,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        Integer size,

        String cursor
) {

    private static final int DEFAULT_SIZE = 20;

    public ResourceReportAdminFindAllRequest {
        status = blankToNull(status);
        kind = blankToNull(kind);
        cursor = blankToNull(cursor);
    }

    public ResourceReportStatus resolvedStatus() {
        if (status == null) {
            return ResourceReportStatus.OPEN;
        }
        return "ALL".equals(status) ? null : ResourceReportStatus.valueOf(status);
    }

    public ResourceReportKind resolvedKind() {
        return kind == null ? null : ResourceReportKind.valueOf(kind);
    }

    public int resolvedSize() {
        return size == null ? DEFAULT_SIZE : size;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
