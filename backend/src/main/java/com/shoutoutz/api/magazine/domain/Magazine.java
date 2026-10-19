package com.shoutoutz.api.magazine.domain;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Magazine {

    private final Long id;
    private String title;
    private String summary;
    private long featuredUserId;
    private String featuredUserIntroduction;
    private Long thumbnailMediaId;
    private String contentMd;
    private final long authorId;
    private MagazineStatus status;
    private Instant publishedAt;
    private final Instant createdAt;
    private final Instant updatedAt;
    private Instant deletedAt;

    public void edit(
            String title,
            String summary,
            long featuredUserId,
            String featuredUserIntroduction,
            Long thumbnailMediaId,
            String contentMd
    ) {
        if (status == MagazineStatus.PUBLISHED) {
            validatePublishable(summary, featuredUserIntroduction, thumbnailMediaId, contentMd);
        }
        this.title = title;
        this.summary = summary;
        this.featuredUserId = featuredUserId;
        this.featuredUserIntroduction = featuredUserIntroduction;
        this.thumbnailMediaId = thumbnailMediaId;
        this.contentMd = contentMd;
    }

    public void publish(Instant now) {
        validatePublishable(summary, featuredUserIntroduction, thumbnailMediaId, contentMd);
        status = MagazineStatus.PUBLISHED;
        if (publishedAt == null) {
            publishedAt = now;
        }
    }

    public void unpublish() {
        status = MagazineStatus.DRAFT;
    }

    public void delete(Instant now) {
        deletedAt = now;
    }

    public boolean isPublished() {
        return status == MagazineStatus.PUBLISHED && deletedAt == null;
    }

    public void validatePublished() {
        if (!isPublished()) {
            throw new EntityNotFoundException(MagazineErrorCode.MAGAZINE_NOT_FOUND);
        }
    }

    private void validatePublishable(
            String summary,
            String featuredUserIntroduction,
            Long thumbnailMediaId,
            String contentMd
    ) {
        if (summary == null || summary.isBlank() || thumbnailMediaId == null
                || contentMd == null || contentMd.isBlank()
                || featuredUserIntroduction == null || featuredUserIntroduction.isBlank()) {
            throw new InvalidInputException(MagazineErrorCode.MAGAZINE_PUBLISH_INCOMPLETE);
        }
    }
}
