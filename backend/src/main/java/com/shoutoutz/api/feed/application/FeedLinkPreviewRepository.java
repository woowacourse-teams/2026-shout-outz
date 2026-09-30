package com.shoutoutz.api.feed.application;

import com.shoutoutz.api.feed.application.dto.LinkPreview;
import com.shoutoutz.api.feed.application.dto.LinkPreviewMetadata;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 피드와 URL별 미리보기 캐시 연결을 저장·조회하는 포트. */
public interface FeedLinkPreviewRepository {

    void link(long feedId, String url);

    void unlink(long feedId);

    boolean hasReference(long feedId);

    Map<Long, LinkPreview> findByFeedIds(List<Long> feedIds);

    Optional<FetchJob> claimDue(Instant now, Instant staleBefore);

    void complete(FetchJob job, LinkPreviewMetadata metadata, Instant now);

    void fail(FetchJob job, Instant nextAttemptAt);

    List<FeedContent> findFeedsAfter(long afterId, int limit);

    int deleteUnusedBefore(Instant cutoff);

    record FetchJob(long id, String url, int attempts) {
    }

    record FeedContent(long id, String content) {
    }
}
