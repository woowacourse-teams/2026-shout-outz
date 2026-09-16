package com.shoutoutz.api.verification.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserVerificationErrorCode implements ErrorCode {

    VERIFICATION_REQUEST_ALREADY_PENDING("이미 심사 대기 중인 인증 신청이 있습니다."),
    VERIFICATION_ALREADY_APPROVED("이미 인증된 사용자는 인증을 신청할 수 없습니다."),
    VERIFICATION_USER_TYPE_INVALID("크루 또는 코치만 인증을 신청할 수 있습니다."),
    VERIFICATION_COURSE_INFO_INVALID("신청 유형에 맞는 우테코 정보를 입력해주세요."),
    VERIFICATION_TRACK_INVALID("트랙 값이 올바르지 않습니다."),
    VERIFICATION_ADMIN_FORBIDDEN("관리자만 크루/코치 인증 신청 목록을 조회할 수 있습니다."),
    VERIFICATION_ADMIN_CURSOR_INVALID(
            "크루/코치 인증 신청 목록 조회 커서가 올바르지 않습니다. 커서 없이 다시 조회해주세요."
    );

    private final String message;
}
