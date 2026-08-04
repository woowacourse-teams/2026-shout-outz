package com.dropit.backend.maker;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MakerRequest(
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 20, message = "이름은 20자 이내여야 합니다.")
    String name,

    @NotBlank(message = "역할 또는 소속은 필수입니다.")
    @Size(max = 40, message = "역할 또는 소속은 40자 이내여야 합니다.")
    String role,

    @NotBlank(message = "소개는 필수입니다.")
    @Size(max = 100, message = "소개는 100자 이내여야 합니다.")
    String bio,

    @Size(max = 2048, message = "아바타 URL이 너무 깁니다.")
    String avatarUrl,

    @Size(max = 20, message = "tone 값이 너무 깁니다.")
    String tone
) {
}
