package com.shoutoutz.api.homebanner.application;

import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.homebanner.domain.HomeBannerErrorCode;
import com.shoutoutz.api.media.application.MediaUrlResolver;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeBannerImageService {

    private final MediaMetadataRepository mediaMetadataRepository;
    private final MediaUrlResolver mediaUrlResolver;

    @Transactional(readOnly = true)
    public URI createImageUrl(long mediaId) {
        MediaMetadata media = findValidMedia(mediaId);
        return mediaUrlResolver.resolve(media, MediaVariant.DISPLAY);
    }

    private MediaMetadata findValidMedia(long mediaId) {
        MediaMetadata media = mediaMetadataRepository.findById(mediaId)
                .orElseThrow(() -> new NotFoundException(HomeBannerErrorCode.HOME_BANNER_MEDIA_NOT_FOUND));
        if (media.getPurpose() != MediaPurpose.HOME_BANNER || media.getStatus() != MediaStatus.READY) {
            throw new ConflictException(HomeBannerErrorCode.HOME_BANNER_MEDIA_INVALID);
        }
        return media;
    }
}
