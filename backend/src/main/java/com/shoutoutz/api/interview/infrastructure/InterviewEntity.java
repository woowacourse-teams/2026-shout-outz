package com.shoutoutz.api.interview.infrastructure;

import com.shoutoutz.api.common.entity.BaseEntity;
import com.shoutoutz.api.interview.domain.Interview;
import com.shoutoutz.api.interview.domain.InterviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "interviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(length = 500)
    private String summary;
    @Column(name = "interviewee_id", nullable = false)
    private long intervieweeId;
    @Column(name = "thumbnail_media_id")
    private Long thumbnailMediaId;
    @Column(name = "content_md", columnDefinition = "TEXT")
    private String contentMd;
    @Column(name = "author_id", nullable = false)
    private long authorId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewStatus status;
    private Instant publishedAt;
    private Instant deletedAt;

    public static InterviewEntity from(Interview interview) {
        InterviewEntity entity = new InterviewEntity();
        entity.authorId = interview.getAuthorId();
        entity.update(interview);
        return entity;
    }

    public void update(Interview interview) {
        title = interview.getTitle();
        summary = interview.getSummary();
        intervieweeId = interview.getIntervieweeId();
        thumbnailMediaId = interview.getThumbnailMediaId();
        contentMd = interview.getContentMd();
        status = interview.getStatus();
        publishedAt = interview.getPublishedAt();
        deletedAt = interview.getDeletedAt();
    }

    public Interview toDomain() {
        return Interview.builder().id(id).title(title).summary(summary).intervieweeId(intervieweeId)
                .thumbnailMediaId(thumbnailMediaId).contentMd(contentMd).authorId(authorId).status(status)
                .publishedAt(publishedAt).createdAt(getCreatedAt()).updatedAt(getUpdatedAt())
                .deletedAt(deletedAt).build();
    }
}
