package com.shoutoutz.api.projectresource.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectResourceErrorCode implements ErrorCode {

    RESOURCE_TOPIC_NOT_FOUND("기술 주제를 찾을 수 없습니다.");

    private final String message;
}
