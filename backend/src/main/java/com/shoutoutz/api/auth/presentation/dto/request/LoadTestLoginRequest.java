package com.shoutoutz.api.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * [부하 테스트 전용] 로그인할 사용자의 handle. @ 접두사는 붙이지 않아도 된다.
 */
public record LoadTestLoginRequest(
        @NotBlank(message = "handle은 필수입니다.")
        String handle
) {
}
