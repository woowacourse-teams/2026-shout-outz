package com.shoutoutz.api.comment.application;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.shoutoutz.api.comment.domain.CommentErrorCode.REACTION_NOT_FOUND;

import com.shoutoutz.api.comment.domain.MagazineComment;
import com.shoutoutz.api.comment.domain.MagazineCommentReactionCounts;
import com.shoutoutz.api.comment.domain.MagazineCommentReactionRepository;
import com.shoutoutz.api.comment.domain.MagazineCommentReactionType;
import com.shoutoutz.api.comment.domain.MagazineCommentRepository;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentReactionResponse;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.magazine.domain.MagazineErrorCode;
import com.shoutoutz.api.magazine.domain.MagazineRepository;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MagazineCommentReactionService {

    private final MagazineRepository magazineRepository;
    private final UserRepository userRepository;
    private final MagazineCommentRepository magazineCommentRepository;
    private final MagazineCommentReactionRepository magazineCommentReactionRepository;

    @Transactional
    public MagazineCommentReactionResponse add(
            long magazineId,
            long commentId,
            long userId,
            String type
    ) {
        MagazineCommentReactionType reactionType = MagazineCommentReactionType.from(type);
        validateActiveUser(userId);
        validateActiveMagazine(magazineId);
        findActiveComment(magazineId, commentId);

        magazineCommentReactionRepository.add(commentId, userId, reactionType);
        return response(magazineId, commentId, reactionType, true);
    }

    @Transactional
    public MagazineCommentReactionResponse remove(
            long magazineId,
            long commentId,
            long userId,
            String type
    ) {
        MagazineCommentReactionType reactionType = MagazineCommentReactionType.from(type);
        validateActiveUser(userId);
        validateActiveMagazine(magazineId);
        findActiveComment(magazineId, commentId);

        if (!magazineCommentReactionRepository.remove(commentId, userId, reactionType)) {
            throw new EntityNotFoundException(REACTION_NOT_FOUND);
        }
        return response(magazineId, commentId, reactionType, false);
    }

    private void validateActiveUser(long userId) {
        userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException(MagazineErrorCode.MAGAZINE_USER_FORBIDDEN));
    }

    private void validateActiveMagazine(long magazineId) {
        magazineRepository.findForUpdate(magazineId)
                .orElseThrow(() -> new EntityNotFoundException(MagazineErrorCode.MAGAZINE_NOT_FOUND))
                .validatePublished();
    }

    private MagazineComment findActiveComment(long magazineId, long commentId) {
        return magazineCommentRepository.findById(commentId)
                .filter(comment -> comment.getMagazineId().equals(magazineId))
                .filter(comment -> !comment.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
    }

    private MagazineCommentReactionResponse response(
            long magazineId,
            long commentId,
            MagazineCommentReactionType type,
            boolean active
    ) {
        MagazineCommentReactionCounts counts = magazineCommentReactionRepository.countByCommentId(commentId);
        return new MagazineCommentReactionResponse(
                magazineId,
                commentId,
                type,
                active,
                counts.agreeCount()
        );
    }
}
