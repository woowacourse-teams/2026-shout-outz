package com.shoutoutz.api.interview.application.dto;

import com.shoutoutz.api.interview.domain.InterviewStatus;
import java.time.Instant;

public record InterviewItem(
        long id,
        String title,
        String summary,
        String contentMd,
        Long thumbnailMediaId,
        Long intervieweeUserId,
        String handle,
        String displayName,
        String bio,
        Long avatarImageId,
        InterviewStatus status,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        long commentCount,
        long viewCount
) {
}
