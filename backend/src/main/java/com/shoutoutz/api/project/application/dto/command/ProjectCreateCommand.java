package com.shoutoutz.api.project.application.dto.command;

import java.util.List;

public record ProjectCreateCommand(
        String title,
        String teamName,
        String tagline,
        int cohort,
        Long thumbnailMediaId,
        String githubRepositoryUrl,
        String deploymentUrl,
        String descriptionMd,
        List<Long> techTagIds,
        List<String> memberHandles,
        Long registeredBy
) {
}
