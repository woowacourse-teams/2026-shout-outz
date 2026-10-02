package com.shoutoutz.api.feed.application;

import com.shoutoutz.api.feed.application.dto.LinkPreview;
import com.shoutoutz.api.feed.application.dto.LinkPreviewMetadata;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 피드·댓글과 URL별 미리보기 캐시 연결을 저장·조회하는 포트. */
public interface FeedLinkPreviewRepository {

    void link(long feedId, String url);

    void unlink(long feedId);

    boolean hasReference(long feedId);

    Map<Long, LinkPreview> findByFeedIds(List<Long> feedIds);

    void linkFeedComment(long commentId, String url);

    void unlinkFeedComment(long commentId);

    boolean hasFeedCommentReference(long commentId);

    Map<Long, LinkPreview> findByFeedCommentIds(List<Long> commentIds);

    void linkProjectComment(long commentId, String url);

    void unlinkProjectComment(long commentId);

    boolean hasProjectCommentReference(long commentId);

    Map<Long, LinkPreview> findByProjectCommentIds(List<Long> commentIds);

    Optional<FetchJob> claimDue(Instant now, Instant staleBefore);

    void complete(FetchJob job, LinkPreviewMetadata metadata, Instant now);

    void fail(FetchJob job, Instant nextAttemptAt);

    List<FeedContent> findFeedsAfter(long afterId, int limit);

    List<CommentContent> findFeedCommentsAfter(long afterId, int limit);

    List<CommentContent> findProjectCommentsAfter(long afterId, int limit);

    int deleteUnusedBefore(Instant cutoff);

    record FetchJob(long id, String url, int attempts) {
    }

    record FeedContent(long id, String content) {
    }

    record CommentContent(long id, String content) {
    }
}
