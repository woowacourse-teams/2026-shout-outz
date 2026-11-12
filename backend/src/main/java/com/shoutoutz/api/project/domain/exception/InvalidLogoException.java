package com.shoutoutz.api.project.domain.exception;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.project.domain.ProjectErrorCode;

/**
 * 프로젝트 로고로 사용할 수 없는 이미지를 입력했을 때 발생하는 예외입니다.
 */
public class InvalidLogoException extends BadRequestException {

    public InvalidLogoException(ProjectErrorCode errorCode) {
        super(errorCode);
    }
}
