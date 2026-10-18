package com.shoutoutz.api.magazine.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MagazineErrorCode implements ErrorCode {
    REACTION_TYPE_INVALID("지원하지 않는 매거진 반응 타입입니다."),
    REACTION_NOT_FOUND("요청한 반응을 찾을 수 없습니다."),
    MAGAZINE_NOT_FOUND("매거진을 찾을 수 없습니다."),
    MAGAZINE_USER_FORBIDDEN("활성 사용자만 매거진의 반응과 댓글을 변경할 수 있습니다."),
    MAGAZINE_ADMIN_FORBIDDEN("활성 관리자만 매거진을 관리할 수 있습니다."),
    FEATURED_USER_NOT_FOUND("매거진 대상 사용자를 찾을 수 없습니다."),
    MAGAZINE_PUBLISH_INCOMPLETE("발행하려면 요약, 인터뷰이 소개문, 본문, 대표 이미지가 필요합니다."),
    MAGAZINE_INVALID_MEDIA("사용할 수 없는 매거진 이미지입니다."),
    MAGAZINE_INVALID_CURSOR("매거진 목록 커서가 올바르지 않습니다."),
    MAGAZINE_INVALID_SIZE("조회 개수는 1 이상 50 이하여야 합니다.");

    private final String message;
}
