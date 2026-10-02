package com.shoutoutz.api.feed.application;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 배포 전 작성된 피드를 한 배치씩 읽어 링크 참조만 기록한다. 외부 HTTP 요청은 하지 않는다. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "link-preview.enabled", havingValue = "true", matchIfMissing = true)
public class LinkPreviewBackfillBatch {

    private static final int BATCH_SIZE = 100;

    private final FeedLinkPreviewRepository repository;
    private final FeedLinkUrlExtractor extractor;

    @Transactional
    public long scanAfter(long lastId) {
        var feeds = repository.findFeedsAfter(lastId, BATCH_SIZE);
        for (var feed : feeds) {
            if (!repository.hasReference(feed.id())) {
                extractor.firstUrl(feed.content()).ifPresent(url -> repository.link(feed.id(), url));
            }
        }
        return feeds.isEmpty() ? -1L : feeds.getLast().id();
    }

    @Transactional
    public long scanFeedCommentsAfter(long lastId) {
        var comments = repository.findFeedCommentsAfter(lastId, BATCH_SIZE);
        for (var comment : comments) {
            if (!repository.hasFeedCommentReference(comment.id())) {
                extractor.firstUrl(comment.content())
                        .ifPresent(url -> repository.linkFeedComment(comment.id(), url));
            }
        }
        return comments.isEmpty() ? -1L : comments.getLast().id();
    }

    @Transactional
    public long scanProjectCommentsAfter(long lastId) {
        var comments = repository.findProjectCommentsAfter(lastId, BATCH_SIZE);
        for (var comment : comments) {
            if (!repository.hasProjectCommentReference(comment.id())) {
                extractor.firstUrl(comment.content())
                        .ifPresent(url -> repository.linkProjectComment(comment.id(), url));
            }
        }
        return comments.isEmpty() ? -1L : comments.getLast().id();
    }
}
