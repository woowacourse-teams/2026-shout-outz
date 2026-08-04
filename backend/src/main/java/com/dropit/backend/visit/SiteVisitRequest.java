package com.dropit.backend.visit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SiteVisitRequest(
    @NotBlank(message = "visitorId는 필수입니다.")
    @Size(max = 128, message = "visitorId는 128자 이내여야 합니다.")
    String visitorId
) {
}
