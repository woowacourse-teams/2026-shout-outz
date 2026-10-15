package com.shoutoutz.api.interview.application;

import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.interview.domain.Interview;
import com.shoutoutz.api.interview.domain.InterviewErrorCode;
import com.shoutoutz.api.media.application.MediaUrlResolver;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.project.domain.DescriptionMediaReferences;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InterviewMediaService {
    private final MediaMetadataRepository repository;
    private final MediaUrlResolver urlResolver;

    public String normalizeContent(Interview existing, String contentMd) {
        if (existing == null) {
            return contentMd;
        }
        Map<Long, URI> urls = urlResolver.resolveAll(DescriptionMediaReferences.extractMediaIds(existing.getContentMd()));
        return urlResolver.replaceDescriptionUrlsWithReferences(contentMd, urls);
    }

    public void validate(Interview existing, Long thumbnailId, String contentMd, long editorId) {
        if (urlResolver.containsUnsupportedDescriptionImageReference(contentMd)
                || containsHtmlImage(contentMd)) {
            throw invalidMedia();
        }
        Set<Long> existingContent = new HashSet<>();
        Long existingThumbnail = null;
        if (existing != null) {
            existingContent.addAll(DescriptionMediaReferences.extractMediaIds(existing.getContentMd()));
            existingThumbnail = existing.getThumbnailMediaId();
        }
        if (thumbnailId != null) {
            validateMedia(thumbnailId, MediaPurpose.INTERVIEW_THUMBNAIL, editorId,
                    thumbnailId.equals(existingThumbnail));
        }
        for (Long id : DescriptionMediaReferences.extractMediaIds(contentMd)) {
            validateMedia(id, MediaPurpose.INTERVIEW_CONTENT, editorId, existingContent.contains(id));
        }
    }

    private void validateMedia(long id, MediaPurpose purpose, long editorId, boolean alreadyAttached) {
        MediaMetadata media = repository.findById(id).orElseThrow(this::invalidMedia);
        if (media.getPurpose() != purpose || media.getStatus() != MediaStatus.READY) {
            throw invalidMedia();
        }
        if (!alreadyAttached && !Long.valueOf(editorId).equals(media.getUploadedBy())) {
            throw invalidMedia();
        }
    }

    private boolean containsHtmlImage(String content) {
        if (content == null) {
            return false;
        }
        return java.util.regex.Pattern.compile("<img\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(content).find();
    }

    private InvalidInputException invalidMedia() {
        return new InvalidInputException(InterviewErrorCode.INTERVIEW_INVALID_MEDIA);
    }
}
