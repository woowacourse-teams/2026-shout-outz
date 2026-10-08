package com.shoutoutz.api.bugreport.application.dto;

import java.time.Instant;

public record BugReportCursor(Instant createdAt, long bugReportId) {
}
