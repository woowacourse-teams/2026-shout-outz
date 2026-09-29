package com.shoutoutz.api.media.application;

import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.presentation.dto.response.MediaStatusResponse;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MediaStatusQueryService {

    private final MediaMetadataRepository mediaMetadataRepository;

    @Transactional(readOnly = true)
    public MediaStatusResponse getStatus(long requesterId, long mediaId) {
        if (mediaId <= 0) {
            throw new BadRequestException(CommonErrorCode.VALIDATION_FAILED);
        }

        MediaMetadata metadata = mediaMetadataRepository.findById(mediaId)
                .orElseThrow(() -> new EntityNotFoundException(CommonErrorCode.RESOURCE_NOT_FOUND));
        if (!Objects.equals(metadata.getUploadedBy(), requesterId)) {
            throw new ForbiddenException(CommonErrorCode.FORBIDDEN);
        }

        return new MediaStatusResponse(metadata.getId(), metadata.getStatus());
    }
}
