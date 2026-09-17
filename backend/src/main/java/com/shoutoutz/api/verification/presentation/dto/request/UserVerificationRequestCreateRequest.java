package com.shoutoutz.api.verification.presentation.dto.request;

import com.shoutoutz.api.user.domain.profile.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.CodePointLength;

public record UserVerificationRequestCreateRequest(
        @NotNull(message = "userType은 필수입니다.")
        UserType userType,

        @NotBlank(message = "nickname은 필수입니다.")
        @CodePointLength(max = 50, message = "nickname은 50자를 초과할 수 없습니다.")
        String nickname,

        @Positive(message = "cohort는 양의 정수여야 합니다.")
        Integer cohort,

        String track
) {

    public UserVerificationRequestCreateRequest {
        nickname = nickname == null ? null : nickname.trim();
        track = track == null ? null : track.trim();
    }
}
