package com.shoutoutz.api.project.presentation.dto.response;

import java.time.Instant;
import java.util.List;

public record AdminProjectMigrationUpdateResponse(
        long projectId,
        List<String> updatedFields,
        Instant updatedAt
) {

    public AdminProjectMigrationUpdateResponse {
        updatedFields = List.copyOf(updatedFields);
    }
}
