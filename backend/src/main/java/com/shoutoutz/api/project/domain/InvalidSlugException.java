package com.shoutoutz.api.project.domain;

import com.shoutoutz.api.common.exception.custom.BadRequestException;

/**
 * 리포지토리 이름으로 slug 규칙을 만족하는 프로젝트 주소를 만들 수 없을 때 발생하는 예외입니다.
 */
public class InvalidSlugException extends BadRequestException {

    public InvalidSlugException() {
        super(ProjectErrorCode.PROJECT_INVALID_SLUG);
    }
}
