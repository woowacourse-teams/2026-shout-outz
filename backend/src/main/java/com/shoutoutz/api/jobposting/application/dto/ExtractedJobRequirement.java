package com.shoutoutz.api.jobposting.application.dto;

import com.shoutoutz.api.jobposting.domain.RequirementCategory;

public record ExtractedJobRequirement(String name, RequirementCategory category, String summary) {
}
