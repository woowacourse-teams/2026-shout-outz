package com.shoutoutz.api.magazine.application;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.magazine.domain.MagazineErrorCode;
import com.shoutoutz.api.magazine.domain.MagazineReactionRepository;
import com.shoutoutz.api.magazine.domain.MagazineReactionType;
import com.shoutoutz.api.magazine.domain.MagazineRepository;
import com.shoutoutz.api.magazine.presentation.dto.response.MagazineReactionResponse;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MagazineReactionService {

    private final MagazineRepository magazineRepository;
    private final UserRepository userRepository;
    private final MagazineReactionRepository magazineReactionRepository;

    @Transactional
    public MagazineReactionResponse add(long magazineId, long userId, String type) {
        MagazineReactionType reactionType = MagazineReactionType.from(type);
        validateActiveUser(userId);
        validateActiveMagazine(magazineId);

        magazineReactionRepository.add(magazineId, userId, reactionType);
        return response(magazineId, reactionType, true);
    }

    @Transactional
    public MagazineReactionResponse remove(long magazineId, long userId, String type) {
        MagazineReactionType reactionType = MagazineReactionType.from(type);
        validateActiveUser(userId);
        validateActiveMagazine(magazineId);

        if (!magazineReactionRepository.remove(magazineId, userId, reactionType)) {
            throw new EntityNotFoundException(MagazineErrorCode.REACTION_NOT_FOUND);
        }
        return response(magazineId, reactionType, false);
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

    private MagazineReactionResponse response(long magazineId, MagazineReactionType type, boolean active) {
        return new MagazineReactionResponse(
                magazineId,
                type,
                active,
                magazineReactionRepository.countByMagazineId(magazineId)
        );
    }
}
