package com.dropit.backend.crew;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrewVerifyRequest(
    @NotBlank(message = "크루 인증 코드는 필수입니다.")
    @Size(max = 80, message = "크루 인증 코드가 너무 깁니다.")
    String code
) {
}
