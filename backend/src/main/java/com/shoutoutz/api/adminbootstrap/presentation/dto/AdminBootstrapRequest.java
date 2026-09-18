package com.shoutoutz.api.adminbootstrap.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminBootstrapRequest(
        @NotBlank(message = "code는 필수입니다.")
        @Size(max = 128, message = "code는 128자를 초과할 수 없습니다.")
        String code
) {
}
