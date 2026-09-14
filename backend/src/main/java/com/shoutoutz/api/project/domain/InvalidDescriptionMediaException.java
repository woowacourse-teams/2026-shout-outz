package com.shoutoutz.api.project.domain;

import com.shoutoutz.api.common.exception.custom.BadRequestException;

/**
 * 프로젝트 본문에 사용할 수 없는 이미지를 참조했을 때 발생하는 예외입니다.
 */
public class InvalidDescriptionMediaException extends BadRequestException {

    public InvalidDescriptionMediaException(ProjectErrorCode errorCode) {
        super(errorCode);
    }
}
