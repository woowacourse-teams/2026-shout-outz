package com.shoutoutz.api.project.domain.exception;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.project.domain.ProjectErrorCode;

/**
 * 프로젝트 목록 조회 커서의 형식이 깨졌거나, 요청한 정렬 기준과 커서의 정렬 기준이 다를 때 발생하는 예외입니다.
 */
public class InvalidProjectCursorException extends BadRequestException {

    public InvalidProjectCursorException() {
        super(ProjectErrorCode.PROJECT_INVALID_CURSOR);
    }
}
