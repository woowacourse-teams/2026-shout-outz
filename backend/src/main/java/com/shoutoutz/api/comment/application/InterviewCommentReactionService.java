package com.shoutoutz.api.comment.application;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.shoutoutz.api.comment.domain.CommentErrorCode.REACTION_NOT_FOUND;

import com.shoutoutz.api.comment.domain.InterviewComment;
import com.shoutoutz.api.comment.domain.InterviewCommentReactionCounts;
import com.shoutoutz.api.comment.domain.InterviewCommentReactionRepository;
import com.shoutoutz.api.comment.domain.InterviewCommentReactionType;
import com.shoutoutz.api.comment.domain.InterviewCommentRepository;
import com.shoutoutz.api.comment.presentation.dto.response.InterviewCommentReactionResponse;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.interview.domain.InterviewErrorCode;
import com.shoutoutz.api.interview.domain.InterviewRepository;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewCommentReactionService {

    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final InterviewCommentRepository interviewCommentRepository;
    private final InterviewCommentReactionRepository interviewCommentReactionRepository;

    @Transactional
    public InterviewCommentReactionResponse add(
            long interviewId,
            long commentId,
            long userId,
            String type
    ) {
        InterviewCommentReactionType reactionType = InterviewCommentReactionType.from(type);
        validateActiveUser(userId);
        validateActiveInterview(interviewId);
        findActiveComment(interviewId, commentId);

        interviewCommentReactionRepository.add(commentId, userId, reactionType);
        return response(interviewId, commentId, reactionType, true);
    }

    @Transactional
    public InterviewCommentReactionResponse remove(
            long interviewId,
            long commentId,
            long userId,
            String type
    ) {
        InterviewCommentReactionType reactionType = InterviewCommentReactionType.from(type);
        validateActiveUser(userId);
        validateActiveInterview(interviewId);
        findActiveComment(interviewId, commentId);

        if (!interviewCommentReactionRepository.remove(commentId, userId, reactionType)) {
            throw new EntityNotFoundException(REACTION_NOT_FOUND);
        }
        return response(interviewId, commentId, reactionType, false);
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

    private InterviewComment findActiveComment(long interviewId, long commentId) {
        return interviewCommentRepository.findById(commentId)
                .filter(comment -> comment.getInterviewId().equals(interviewId))
                .filter(comment -> !comment.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
    }

    private InterviewCommentReactionResponse response(
            long interviewId,
            long commentId,
            InterviewCommentReactionType type,
            boolean active
    ) {
        InterviewCommentReactionCounts counts = interviewCommentReactionRepository.countByCommentId(commentId);
        return new InterviewCommentReactionResponse(
                interviewId,
                commentId,
                type,
                active,
                counts.agreeCount()
        );
    }
}
