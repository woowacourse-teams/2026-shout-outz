package com.shoutoutz.api.project.domain;

import com.shoutoutz.api.common.exception.custom.BadRequestException;

/**
 * 프로젝트 썸네일로 사용할 수 없는 이미지를 입력했을 때 발생하는 예외입니다.
 */
public class InvalidThumbnailException extends BadRequestException {

    public InvalidThumbnailException(ProjectErrorCode errorCode) {
        super(errorCode);
    }
}
