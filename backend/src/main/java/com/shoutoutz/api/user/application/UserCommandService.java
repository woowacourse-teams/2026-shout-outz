package com.shoutoutz.api.user.application;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.exception.UserErrorCode;
import com.shoutoutz.api.user.presentation.dto.request.UserProfileUpdateRequest;
import com.shoutoutz.api.user.presentation.dto.response.UserProfileUpdateResponse;
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
    public UserProfileUpdateResponse updateMyProfile(
            long userId,
            UserProfileUpdateRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_PROFILE_NOT_FOUND));

        if (!profile.canChangeDisplayNameTo(request.displayName())) {
            throw new BadRequestException(UserErrorCode.PROFILE_DISPLAY_NAME_IMMUTABLE);
        }
        validateAvatarImage(userId, request.avatarImageId());

        UserProfile updatedProfile = profile.update(
                request.displayName(),
                request.bio(),
                request.avatarImageId(),
                request.githubProfileUrl(),
                request.blogUrl()
        );
        UserProfile savedProfile = userProfileRepository.save(updatedProfile);

        return new UserProfileUpdateResponse(
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
