package com.shoutoutz.api.user.application;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.user.application.command.UserProfileUpdateCommand;
import com.shoutoutz.api.user.application.command.UserProfileUpdateResult;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final MediaMetadataRepository mediaMetadataRepository;

    @Transactional
    public UserProfileUpdateResult updateMyProfile(UserProfileUpdateCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));
        UserProfile profile = userProfileRepository.findByUserId(command.userId())
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_PROFILE_NOT_FOUND));

        if (!profile.canChangeDisplayNameTo(command.displayName())) {
            throw new BadRequestException(UserErrorCode.PROFILE_DISPLAY_NAME_IMMUTABLE);
        }
        validateAvatarImage(command.userId(), command.avatarImageId());

        UserProfile updatedProfile = profile.update(
                command.displayName(),
                command.bio(),
                command.avatarImageId(),
                command.githubProfileUrl(),
                command.blogUrl()
        );
        UserProfile savedProfile = userProfileRepository.save(updatedProfile);

        return new UserProfileUpdateResult(
                user.getId(),
                user.getHandle().value(),
                savedProfile.getDisplayName().value(),
                savedProfile.getUserType(),
                savedProfile.getTrack(),
                savedProfile.getCohort(),
                savedProfile.getBio(),
                savedProfile.getAvatarImageId(),
                savedProfile.getGithubProfileUrl(),
                savedProfile.getBlogUrl()
        );
    }

    private void validateAvatarImage(long userId, Long avatarImageId) {
        if (avatarImageId == null) {
            return;
        }

        MediaMetadata metadata = mediaMetadataRepository.findById(avatarImageId)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.AVATAR_IMAGE_NOT_FOUND));
        if (metadata.getUploadedBy() == null || metadata.getUploadedBy() != userId) {
            throw new ForbiddenException(UserErrorCode.AVATAR_IMAGE_FORBIDDEN);
        }
        if (metadata.getPurpose() != MediaPurpose.USER_AVATAR) {
            throw new BadRequestException(UserErrorCode.AVATAR_IMAGE_INVALID_PURPOSE);
        }
        if (metadata.getStatus() != MediaStatus.READY) {
            throw new ConflictException(UserErrorCode.AVATAR_IMAGE_NOT_READY);
        }
    }
}
