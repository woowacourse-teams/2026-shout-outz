package com.shoutoutz.api.user.domain.account;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND("사용자를 찾을 수 없습니다."),
    HANDLE_ALREADY_EXISTS("이미 사용 중인 handle입니다."),
    USER_HANDLE_REQUIRED("사용자 handle은 필수입니다."),
    USER_STATUS_REQUIRED("사용자 상태는 필수입니다."),
    USER_ROLE_REQUIRED("사용자 권한은 필수입니다."),
    USER_DELETION_STATE_INVALID("탈퇴 상태와 탈퇴 시각은 함께 존재해야 합니다."),
    USER_LOGIN_AT_REQUIRED("로그인 시각은 필수입니다."),
    USER_LOGIN_BANNED("정지된 사용자는 로그인할 수 없습니다."),
    USER_LOGIN_PURGED("개인정보가 파기된 사용자는 복구할 수 없습니다."),
    USER_SEARCH_CURSOR_INVALID("유효하지 않은 사용자 검색 커서입니다."),
    USER_SEARCH_CURSOR_ENCODING_FAILED("사용자 검색 커서를 생성할 수 없습니다.");

    private final String message;
}
