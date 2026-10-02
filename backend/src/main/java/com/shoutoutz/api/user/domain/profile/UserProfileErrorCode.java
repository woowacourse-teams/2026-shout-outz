package com.shoutoutz.api.user.domain.profile;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserProfileErrorCode implements ErrorCode {

    USER_PROFILE_NOT_FOUND("사용자 프로필을 찾을 수 없습니다."),
    USER_ID_REQUIRED("사용자 ID는 필수입니다."),
    DISPLAY_NAME_REQUIRED("사용자 표시 이름은 필수입니다."),
    USER_TYPE_REQUIRED("사용자 유형은 필수입니다."),
    INVALID_TRACK("정의되지 않은 우아한테크코스 트랙입니다."),
    GENERAL_USER_COURSE_INFO_NOT_ALLOWED("일반 사용자는 트랙과 기수를 가질 수 없습니다."),
    CREW_COURSE_INFO_REQUIRED("우아한테크코스 크루는 트랙과 기수가 필요합니다."),
    COACH_COHORT_NOT_ALLOWED("우아한테크코스 코치는 기수를 가질 수 없습니다."),
    PROFILE_DISPLAY_NAME_IMMUTABLE("인증된 우아한테크코스 사용자는 표시 이름을 변경할 수 없습니다."),
    AVATAR_IMAGE_NOT_FOUND("프로필 이미지를 찾을 수 없습니다."),
    AVATAR_IMAGE_FORBIDDEN("본인이 업로드한 이미지만 프로필 이미지로 사용할 수 있습니다."),
    AVATAR_IMAGE_INVALID_PURPOSE("프로필 이미지 용도로 업로드한 이미지만 사용할 수 있습니다."),
    AVATAR_IMAGE_NOT_READY("프로필 이미지 처리가 완료되지 않았습니다.");

    private final String message;
}
