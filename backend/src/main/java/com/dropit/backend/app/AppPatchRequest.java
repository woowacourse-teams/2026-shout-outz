package com.dropit.backend.app;

import java.util.List;

import jakarta.validation.constraints.Size;

public record AppPatchRequest(
    @Size(max = 40, message = "서비스 이름은 40자 이내여야 합니다.")
    String name,
    @Size(max = 80, message = "한 줄 소개는 80자 이내여야 합니다.")
    String tagline,
    @Size(max = 5000, message = "상세 설명은 5000자 이내여야 합니다.")
    String description,
    @Size(max = 2048, message = "서비스 주소가 너무 깁니다.")
    String appUrl,
    @Size(max = 2048, message = "GitHub 주소가 너무 깁니다.")
    String githubUrl,
    @Size(min = 1, max = 2, message = "카테고리는 1~2개여야 합니다.")
    List<AppCategory> categories,
    ThumbnailVariant thumbnailVariant,
    @Size(max = 2048, message = "썸네일 주소가 너무 깁니다.")
    String thumbnailUrl,
    @Size(max = 5, message = "기술 태그는 최대 5개까지 등록할 수 있습니다.")
    List<@Size(max = 30, message = "기술 태그는 30자 이내여야 합니다.") String> techTags
) {
}
