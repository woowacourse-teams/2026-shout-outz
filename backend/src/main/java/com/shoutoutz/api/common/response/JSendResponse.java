package com.shoutoutz.api.common.response;

import java.util.List;

/**
 * JSend 형식의 API 응답입니다.
 *
 * @param <T> 응답 데이터의 타입
 */
public record JSendResponse<T>(
        String status,
        String code,
        String message,
        T data
) {

    public static <T> JSendResponse<T> success(String message, T data) {
        return new JSendResponse<>("success", "SUCCESS", message, data);
    }

    public static <T> JSendResponse<T> fail(String code, String message, T data) {
        return new JSendResponse<>("fail", code, message, data);
    }

    public static <T> JSendResponse<T> error(String code, String message, T data) {
        return new JSendResponse<>("error", code, message, data);
    }

    public record ValidationData(List<ValidationField> fields) {
        public ValidationData {
            fields = List.copyOf(fields);
        }
    }

    public record ValidationField(String field, String reason) {
    }
}
