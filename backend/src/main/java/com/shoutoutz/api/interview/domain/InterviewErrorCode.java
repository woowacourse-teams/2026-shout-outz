package com.shoutoutz.api.interview.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InterviewErrorCode implements ErrorCode {
    INTERVIEW_NOT_FOUND("인터뷰를 찾을 수 없습니다."),
    INTERVIEW_ADMIN_FORBIDDEN("활성 관리자만 인터뷰를 관리할 수 있습니다."),
    INTERVIEWEE_NOT_FOUND("인터뷰 대상 사용자를 찾을 수 없습니다."),
    INTERVIEW_PUBLISH_INCOMPLETE("발행하려면 요약, 본문, 대표 이미지가 필요합니다."),
    INTERVIEW_INVALID_MEDIA("사용할 수 없는 인터뷰 이미지입니다."),
    INTERVIEW_INVALID_CURSOR("인터뷰 목록 커서가 올바르지 않습니다."),
    INTERVIEW_INVALID_SIZE("조회 개수는 1 이상 50 이하여야 합니다.");

    private final String message;
}
