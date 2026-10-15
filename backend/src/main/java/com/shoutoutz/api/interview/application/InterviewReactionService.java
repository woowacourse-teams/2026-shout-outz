package com.shoutoutz.api.interview.application;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.interview.domain.InterviewErrorCode;
import com.shoutoutz.api.interview.domain.InterviewReactionRepository;
import com.shoutoutz.api.interview.domain.InterviewReactionType;

import com.shoutoutz.api.interview.presentation.dto.response.InterviewReactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewReactionService {

    private final InterviewAccessService accessService;
    private final InterviewReactionRepository interviewReactionRepository;

    @Transactional
    public InterviewReactionResponse add(long interviewId, long userId, String type) {
        InterviewReactionType reactionType = InterviewReactionType.from(type);
        accessService.requireActiveUser(userId);
        validateActiveInterview(interviewId);

        interviewReactionRepository.add(interviewId, userId, reactionType);
        return response(interviewId, reactionType, true);
    }

    @Transactional
    public InterviewReactionResponse remove(long interviewId, long userId, String type) {
        InterviewReactionType reactionType = InterviewReactionType.from(type);
        accessService.requireActiveUser(userId);
        validateActiveInterview(interviewId);

        if (!interviewReactionRepository.remove(interviewId, userId, reactionType)) {
            throw new EntityNotFoundException(InterviewErrorCode.REACTION_NOT_FOUND);
        }
        return response(interviewId, reactionType, false);
    }

    private void validateActiveInterview(long interviewId) {
        accessService.lockPublished(interviewId);
    }

    private InterviewReactionResponse response(long interviewId, InterviewReactionType type, boolean active) {
        return new InterviewReactionResponse(
                interviewId,
                type,
                active,
                interviewReactionRepository.countByInterviewId(interviewId)
        );
    }
}
