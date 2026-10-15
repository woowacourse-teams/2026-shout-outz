package com.shoutoutz.api.interview.presentation.dto.response;

import java.net.URI;

public record IntervieweeResponse(String handle, String displayName, String bio, URI avatarUrl) {
}
