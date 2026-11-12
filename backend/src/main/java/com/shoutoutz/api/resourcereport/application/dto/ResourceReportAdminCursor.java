package com.shoutoutz.api.resourcereport.application.dto;

import java.time.Instant;

public record ResourceReportAdminCursor(Instant createdAt, long reportId) {
}
