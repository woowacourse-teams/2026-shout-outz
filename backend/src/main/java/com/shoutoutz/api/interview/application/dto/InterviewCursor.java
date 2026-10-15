package com.shoutoutz.api.interview.application.dto;

import java.time.Instant;

public record InterviewCursor(
        Instant timestamp,
        long id
) {
}
