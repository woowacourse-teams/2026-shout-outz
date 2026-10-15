package com.shoutoutz.api.interview.domain;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Interview {

    private final Long id;
    private String title;
    private String summary;
    private long intervieweeId;
    private Long thumbnailMediaId;
    private String contentMd;
    private final long authorId;
    private InterviewStatus status;
    private Instant publishedAt;
    private final Instant createdAt;
    private final Instant updatedAt;
    private Instant deletedAt;

    public void edit(String title, String summary, long intervieweeId, Long thumbnailMediaId, String contentMd) {
        if (status == InterviewStatus.PUBLISHED) {
            validatePublishable(summary, thumbnailMediaId, contentMd);
        }
        this.title = title;
        this.summary = summary;
        this.intervieweeId = intervieweeId;
        this.thumbnailMediaId = thumbnailMediaId;
        this.contentMd = contentMd;
    }

    public void publish(Instant now) {
        validatePublishable(summary, thumbnailMediaId, contentMd);
        status = InterviewStatus.PUBLISHED;
        if (publishedAt == null) {
            publishedAt = now;
        }
    }

    public void unpublish() {
        status = InterviewStatus.DRAFT;
    }

    public void delete(Instant now) {
        deletedAt = now;
    }

    public boolean isPublished() {
        return status == InterviewStatus.PUBLISHED && deletedAt == null;
    }

    public void validatePublished() {
        if (!isPublished()) {
            throw new EntityNotFoundException(InterviewErrorCode.INTERVIEW_NOT_FOUND);
        }
    }

    private void validatePublishable(String summary, Long thumbnailMediaId, String contentMd) {
        if (summary == null || summary.isBlank() || thumbnailMediaId == null
                || contentMd == null || contentMd.isBlank()) {
            throw new InvalidInputException(InterviewErrorCode.INTERVIEW_PUBLISH_INCOMPLETE);
        }
    }
}
