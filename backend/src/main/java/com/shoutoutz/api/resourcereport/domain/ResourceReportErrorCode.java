package com.shoutoutz.api.resourcereport.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResourceReportErrorCode implements ErrorCode {

    RESOURCE_REPORT_NOT_FOUND("자료 제보를 찾을 수 없습니다."),
    RESOURCE_REPORT_FORBIDDEN("이 프로젝트 팀원만 자료를 제보할 수 있습니다."),
    RESOURCE_REPORT_ADMIN_FORBIDDEN("관리자만 자료 제보를 관리할 수 있습니다."),
    RESOURCE_REPORT_ADMIN_CURSOR_INVALID("자료 제보 목록 조회 커서가 올바르지 않습니다. 커서 없이 다시 조회해주세요."),
    RESOURCE_REPORT_RESOURCE_NOT_FOUND("제보할 자료를 찾을 수 없습니다."),
    RESOURCE_REPORT_RESOURCE_NOT_REMOVABLE("이 프로젝트의 PR이나 팀 문서만 필요 없다고 제보할 수 있습니다."),
    RESOURCE_REPORT_URL_INVALID("GitHub의 PR이나 문서 주소를 넣어 주세요."),
    RESOURCE_REPORT_URL_OUTSIDE_REPOSITORY("이 프로젝트 리포지토리의 PR이나 문서 주소를 넣어 주세요."),
    RESOURCE_REPORT_MEMO_INVALID("제보 이유는 500자 이하여야 합니다."),
    RESOURCE_REPORT_STATUS_INVALID("자료 제보 상태가 올바르지 않습니다.");

    private final String message;
}
