package com.shoutoutz.api.verification.presentation.dto.response;

public record AdminVerificationRequestDecisionActor(
        long userId,
        String handle
) {
}
