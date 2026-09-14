package com.shoutoutz.api.project.domain.exception;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.project.domain.ProjectErrorCode;

/**
 * 프로젝트에 선택할 수 없는 기술 스택이 포함되었을 때 발생하는 예외입니다.
 */
public class InvalidTechTagException extends BadRequestException {

    public InvalidTechTagException(ProjectErrorCode errorCode) {
        super(errorCode);
    }
}
