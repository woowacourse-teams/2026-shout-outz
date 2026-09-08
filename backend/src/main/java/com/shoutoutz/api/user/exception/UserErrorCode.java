package com.shoutoutz.api.user.exception;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND("사용자를 찾을 수 없습니다."),
    USER_PROFILE_NOT_FOUND("사용자 프로필을 찾을 수 없습니다."),
    PROFILE_DISPLAY_NAME_IMMUTABLE("인증된 우아한테크코스 사용자는 표시 이름을 변경할 수 없습니다."),
    AVATAR_IMAGE_NOT_FOUND("프로필 이미지를 찾을 수 없습니다."),
    AVATAR_IMAGE_FORBIDDEN("본인이 업로드한 이미지만 프로필 이미지로 사용할 수 있습니다."),
    AVATAR_IMAGE_INVALID_PURPOSE("프로필 이미지 용도로 업로드한 이미지만 사용할 수 있습니다."),
    AVATAR_IMAGE_NOT_READY("프로필 이미지 처리가 완료되지 않았습니다.");

    private final String message;
}
