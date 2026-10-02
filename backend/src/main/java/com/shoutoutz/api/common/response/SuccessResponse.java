package com.shoutoutz.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 성공한 API 요청의 공통 응답.
 *
 * 모든 API의 성공 응답은 해당 양식을 준수해야 한다.
 *
 * @param <T> 응답 데이터의 타입
 */
public record SuccessResponse<T>(
        String status,
        T data,
        @JsonInclude(JsonInclude.Include.NON_NULL) Object meta
) {

    public static <T> SuccessResponse<T> success(T data) {
        return new SuccessResponse<>("success", data, null);
    }

    public static <T> SuccessResponse<T> success(T data, Object meta) {
        return new SuccessResponse<>("success", data, meta);
    }
}
