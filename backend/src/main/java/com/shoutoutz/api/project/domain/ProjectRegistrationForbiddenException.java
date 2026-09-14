package com.shoutoutz.api.project.domain;

import com.shoutoutz.api.common.exception.custom.ForbiddenException;

/**
 * 우아한테크코스 크루나 코치가 아닌 사용자가 프로젝트를 등록하려 할 때 발생하는 예외입니다.
 */
public class ProjectRegistrationForbiddenException extends ForbiddenException {

    public ProjectRegistrationForbiddenException() {
        super(ProjectErrorCode.PROJECT_REGISTRATION_FORBIDDEN);
    }
}
