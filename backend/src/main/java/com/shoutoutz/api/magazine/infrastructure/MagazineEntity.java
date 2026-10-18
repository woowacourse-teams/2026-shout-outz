package com.shoutoutz.api.magazine.infrastructure;

import com.shoutoutz.api.common.entity.BaseEntity;
import com.shoutoutz.api.magazine.domain.Magazine;
import com.shoutoutz.api.magazine.domain.MagazineStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "magazines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MagazineEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String summary;

    @Column(name = "featured_user_id", nullable = false)
    private long featuredUserId;

    @Column(name = "featured_user_introduction", length = 1000)
    private String featuredUserIntroduction;

    @Column(name = "thumbnail_media_id")
    private Long thumbnailMediaId;

    @Column(name = "content_md", columnDefinition = "TEXT")
    private String contentMd;

    @Column(name = "author_id", nullable = false)
    private long authorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MagazineStatus status;
    private Instant publishedAt;
    private Instant deletedAt;

    public static MagazineEntity from(Magazine magazine) {
        MagazineEntity entity = new MagazineEntity();
        entity.authorId = magazine.getAuthorId();
        entity.update(magazine);
        return entity;
    }

    public void update(Magazine magazine) {
        title = magazine.getTitle();
        summary = magazine.getSummary();
        featuredUserId = magazine.getFeaturedUserId();
        featuredUserIntroduction = magazine.getFeaturedUserIntroduction();
        thumbnailMediaId = magazine.getThumbnailMediaId();
        contentMd = magazine.getContentMd();
        status = magazine.getStatus();
        publishedAt = magazine.getPublishedAt();
        deletedAt = magazine.getDeletedAt();
    }

    public Magazine toDomain() {
        return Magazine.builder()
                .id(id)
                .title(title)
                .summary(summary)
                .featuredUserId(featuredUserId)
                .featuredUserIntroduction(featuredUserIntroduction)
                .thumbnailMediaId(thumbnailMediaId)
                .contentMd(contentMd)
                .authorId(authorId)
                .status(status)
                .publishedAt(publishedAt)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .deletedAt(deletedAt)
                .build();
    }
}
