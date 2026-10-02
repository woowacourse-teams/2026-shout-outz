package com.shoutoutz.api.project.domain.exception;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.project.domain.ProjectErrorCode;

/**
 * 프로젝트 팀원 목록에 추가할 수 없는 사용자가 포함되었을 때 발생하는 예외입니다.
 */
public class InvalidProjectMemberException extends BadRequestException {

    public InvalidProjectMemberException(ProjectErrorCode errorCode) {
        super(errorCode);
    }
}
