package com.shoutoutz.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 실패한 API 요청의 공통 오류 응답.
 * 해당 객체는 GlobalExceptionHanlder에서 생성하여 반환한다.
 */
public record ErrorResponse(
        String status,
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<ErrorDetail> details
) {

    public ErrorResponse {
        details = details == null || details.isEmpty() ? null : List.copyOf(details);
    }

    public static ErrorResponse error(String code, String message) {
        return new ErrorResponse("error", code, message, null);
    }

    public static ErrorResponse error(String code, String message, List<ErrorDetail> details) {
        return new ErrorResponse("error", code, message, details);
    }

    public record ErrorDetail(String field, String message) {
    }
}
