package com.shoutoutz.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 클라이언트와 통신하는 공통 API 응답입니다.
 *
 * @param <T> 응답 데이터의 타입
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JSendResponse<T>(
        String status,
        T data,
        Object meta,
        String code,
        String message,
        List<ErrorDetail> details
) {

    public JSendResponse {
        details = details == null || details.isEmpty() ? null : List.copyOf(details);
    }

    public static <T> JSendResponse<T> success(T data) {
        return new JSendResponse<>("success", data, null, null, null, null);
    }

    public static <T> JSendResponse<T> success(T data, Object meta) {
        return new JSendResponse<>("success", data, meta, null, null, null);
    }

    public static <T> JSendResponse<T> error(String code, String message) {
        return new JSendResponse<>("error", null, null, code, message, null);
    }

    public static <T> JSendResponse<T> error(
            String code, String message, List<ErrorDetail> details) {
        return new JSendResponse<>("error", null, null, code, message, details);
    }

    public record ErrorDetail(String field, String message) {
    }
}
