package com.shoutoutz.api.interview.application;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.interview.domain.InterviewErrorCode;
import com.shoutoutz.api.interview.domain.InterviewReactionRepository;
import com.shoutoutz.api.interview.domain.InterviewReactionType;
import com.shoutoutz.api.interview.domain.InterviewRepository;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewReactionResponse;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewReactionService {

    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final InterviewReactionRepository interviewReactionRepository;

    @Transactional
    public InterviewReactionResponse add(long interviewId, long userId, String type) {
        InterviewReactionType reactionType = InterviewReactionType.from(type);
        validateActiveUser(userId);
        validateActiveInterview(interviewId);

        interviewReactionRepository.add(interviewId, userId, reactionType);
        return response(interviewId, reactionType, true);
    }

    @Transactional
    public InterviewReactionResponse remove(long interviewId, long userId, String type) {
        InterviewReactionType reactionType = InterviewReactionType.from(type);
        validateActiveUser(userId);
        validateActiveInterview(interviewId);

        if (!interviewReactionRepository.remove(interviewId, userId, reactionType)) {
            throw new EntityNotFoundException(InterviewErrorCode.REACTION_NOT_FOUND);
        }
        return response(interviewId, reactionType, false);
    }

    private void validateActiveUser(long userId) {
        userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException(InterviewErrorCode.INTERVIEW_USER_FORBIDDEN));
    }

    private void validateActiveInterview(long interviewId) {
        interviewRepository.findForUpdate(interviewId)
                .orElseThrow(() -> new EntityNotFoundException(InterviewErrorCode.INTERVIEW_NOT_FOUND))
                .validatePublished();
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
