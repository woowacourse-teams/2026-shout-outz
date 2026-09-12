package com.shoutoutz.api.user.domain.profile;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
enum UserProfileErrorCode implements ErrorCode {

    USER_ID_REQUIRED("사용자 ID는 필수입니다."),
    DISPLAY_NAME_REQUIRED("사용자 표시 이름은 필수입니다."),
    USER_TYPE_REQUIRED("사용자 유형은 필수입니다."),
    GENERAL_USER_COURSE_INFO_NOT_ALLOWED("일반 사용자는 트랙과 기수를 가질 수 없습니다."),
    CREW_COURSE_INFO_REQUIRED("우아한테크코스 크루는 트랙과 기수가 필요합니다."),
    COACH_COHORT_NOT_ALLOWED("우아한테크코스 코치는 기수를 가질 수 없습니다."),
    DISPLAY_NAME_CHANGE_NOT_ALLOWED("인증된 우아한테크코스 사용자는 표시 이름을 변경할 수 없습니다.");

    private final String message;
}
