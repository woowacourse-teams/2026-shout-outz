package com.shoutoutz.api.media.presentation.dto.response;

import com.shoutoutz.api.media.domain.MediaStatus;

public record MediaStatusResponse(
        Long mediaId,
        MediaStatus status
) {
}
