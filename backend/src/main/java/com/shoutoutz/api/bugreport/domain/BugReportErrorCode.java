package com.shoutoutz.api.bugreport.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BugReportErrorCode implements ErrorCode {

    BUG_REPORT_NOT_FOUND("버그 제보를 찾을 수 없습니다."),
    BUG_REPORT_ADMIN_FORBIDDEN("관리자만 버그 제보를 관리할 수 있습니다."),
    BUG_REPORT_ADMIN_CURSOR_INVALID("버그 제보 목록 조회 커서가 올바르지 않습니다. 커서 없이 다시 조회해주세요."),
    BUG_REPORT_CONTENT_INVALID("버그 제보 내용이 올바르지 않습니다."),
    BUG_REPORT_STATUS_INVALID("버그 제보 상태가 올바르지 않습니다.");

    private final String message;
}
