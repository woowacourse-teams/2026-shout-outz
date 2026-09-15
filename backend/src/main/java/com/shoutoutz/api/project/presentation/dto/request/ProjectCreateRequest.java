package com.shoutoutz.api.project.presentation.dto.request;

import com.shoutoutz.api.project.domain.DeploymentUrl;
import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.hibernate.validator.constraints.CodePointLength;

public record ProjectCreateRequest(
        @NotBlank(message = "title은 필수입니다.")
        @CodePointLength(max = 100, message = "title은 100자를 초과할 수 없습니다.")
        String title,

        @NotBlank(message = "teamName은 필수입니다.")
        @CodePointLength(max = 50, message = "teamName은 50자를 초과할 수 없습니다.")
        String teamName,

        @NotBlank(message = "tagline은 필수입니다.")
        @CodePointLength(max = 200, message = "tagline은 200자를 초과할 수 없습니다.")
        String tagline,

        @NotNull(message = "cohort는 필수입니다.")
        Integer cohort,

        Long thumbnailMediaId,

        @NotBlank(message = "githubRepositoryUrl은 필수입니다.")
        @CodePointLength(max = 2_048, message = "githubRepositoryUrl은 2,048자를 초과할 수 없습니다.")
        @Pattern(regexp = GithubRepositoryUrl.REGEX,
                message = "githubRepositoryUrl은 https://github.com/{owner}/{repo} 형식이어야 합니다.")
        String githubRepositoryUrl,

        @CodePointLength(max = 2_048, message = "deploymentUrl은 2,048자를 초과할 수 없습니다.")
        @Pattern(regexp = DeploymentUrl.REGEX,
                message = "deploymentUrl은 http 또는 https URL 형식이어야 합니다.")
        String deploymentUrl,

        @CodePointLength(max = 100_000, message = "descriptionMd는 100,000자를 초과할 수 없습니다.")
        String descriptionMd,

        @NotEmpty(message = "techTagIds는 1개 이상이어야 합니다.")
        List<@NotNull(message = "techTagIds에 null을 넣을 수 없습니다.") Long> techTagIds,

        @NotEmpty(message = "memberHandles는 1개 이상이어야 합니다.")
        List<@NotBlank(message = "memberHandles에 빈 값을 넣을 수 없습니다.") String> memberHandles
) {

    /**
     * 배포 URL 입력칸을 비워 보내면 입력하지 않은 것으로 보고 null 로 둔다.
     */
    public ProjectCreateRequest {
        if (deploymentUrl != null && deploymentUrl.isBlank()) {
            deploymentUrl = null;
        }
    }
}
