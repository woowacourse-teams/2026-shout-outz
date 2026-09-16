package com.shoutoutz.api.comment.infrastructure;

import com.shoutoutz.api.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feed_comments")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FeedCommentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // API 식별자는 feedId, 데이터베이스 컬럼명은 feed_id를 사용한다.
    @Column(name = "feed_id", nullable = false)
    private Long feedId;

    // TODO: JPA 연관관계로 매핑할지 논의
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
