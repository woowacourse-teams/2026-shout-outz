package com.shoutoutz.api.news.infrastructure;

import com.shoutoutz.api.common.entity.BaseEntity;
import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.enums.NewsType;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "news")
@DynamicUpdate
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NewsEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NewsType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 200)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_name", nullable = false, length = 50)
    private String authorName;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "event_start_at")
    private Instant eventStartAt;

    @Column(name = "event_end_at")
    private Instant eventEndAt;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned;

    @Column(name = "pin_order")
    private Integer pinOrder;

    @Column(name = "cta_label", length = 100)
    private String ctaLabel;

    @Column(name = "cta_url", columnDefinition = "TEXT")
    private String ctaUrl;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * 수정 API가 허용한 컬럼만 변경한다. 유형, 작성자, 게시 시각, 핀 상태와 삭제 상태는 보존한다.
     */
    public void update(News news) {
        this.title = news.getTitle();
        this.summary = news.getSummary();
        this.body = news.getBody();
        this.authorName = news.getAuthorName();
        this.eventStartAt = news.getEventStartAt();
        this.eventEndAt = news.getEventEndAt();
        this.ctaLabel = news.getCta() == null ? null : news.getCta().label();
        this.ctaUrl = news.getCta() == null ? null : news.getCta().url();
    }
}
