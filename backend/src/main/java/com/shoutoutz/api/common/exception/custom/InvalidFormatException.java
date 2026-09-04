package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 입력 데이터가 기대한 형식과 일치하지 않을 때 발생하는 예외입니다.
 *
 * 예: 날짜, UUID, Enum 등의 값을 올바른 형식으로 변환할 수 없는 경우
 */
@Getter
@RequiredArgsConstructor
public class InvalidFormatException extends RuntimeException {

    private final ErrorCode errorCode;
}
