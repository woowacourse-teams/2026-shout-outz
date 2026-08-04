package com.dropit.backend.app;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AppRequest(
    @NotBlank(message = "서비스 이름은 필수입니다.")
    @Size(max = 40, message = "서비스 이름은 40자 이내여야 합니다.")
    String name,

    @NotBlank(message = "한 줄 소개는 필수입니다.")
    @Size(max = 80, message = "한 줄 소개는 80자 이내여야 합니다.")
    String tagline,

    @Size(max = 5000, message = "상세 설명은 5000자 이내여야 합니다.")
    String description,

    @NotBlank(message = "서비스 주소는 필수입니다.")
    @Size(max = 2048, message = "서비스 주소가 너무 깁니다.")
    String appUrl,

    @Size(max = 2048, message = "GitHub 주소가 너무 깁니다.")
    String githubUrl,

    @NotEmpty(message = "카테고리는 하나 이상 선택해야 합니다.")
    @Size(max = 2, message = "카테고리는 최대 2개까지 선택할 수 있습니다.")
    List<@NotNull AppCategory> categories,

    @NotNull(message = "썸네일 변형은 필수입니다.")
    ThumbnailVariant thumbnailVariant,

    @Size(max = 2048, message = "썸네일 주소가 너무 깁니다.")
    String thumbnailUrl,

    @Size(max = 5, message = "기술 태그는 최대 5개까지 등록할 수 있습니다.")
    List<@NotBlank(message = "기술 태그는 비어 있을 수 없습니다.") @Size(max = 30, message = "기술 태그는 30자 이내여야 합니다.") String> techTags
) {
}
