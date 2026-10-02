package com.shoutoutz.api.feed.application;

import com.shoutoutz.api.feed.application.dto.LinkPreview;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedLinkPreviewService {

    private final FeedLinkUrlExtractor urlExtractor;
    private final FeedLinkPreviewRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    /** 호출자의 피드 저장 트랜잭션에 링크 참조를 함께 기록한다. */
    public void sync(long feedId, String content) {
        sync(content,
                url -> repository.link(feedId, url),
                () -> repository.unlink(feedId));
    }

    public void unlink(long feedId) {
        repository.unlink(feedId);
    }

    public void syncFeedComment(long commentId, String content) {
        sync(content,
                url -> repository.linkFeedComment(commentId, url),
                () -> repository.unlinkFeedComment(commentId));
    }

    public void unlinkFeedComment(long commentId) {
        repository.unlinkFeedComment(commentId);
    }

    public Map<Long, LinkPreview> findByFeedCommentIds(List<Long> commentIds) {
        return repository.findByFeedCommentIds(commentIds);
    }

    public void syncProjectComment(long commentId, String content) {
        sync(content,
                url -> repository.linkProjectComment(commentId, url),
                () -> repository.unlinkProjectComment(commentId));
    }

    public void unlinkProjectComment(long commentId) {
        repository.unlinkProjectComment(commentId);
    }

    public Map<Long, LinkPreview> findByProjectCommentIds(List<Long> commentIds) {
        return repository.findByProjectCommentIds(commentIds);
    }

    private void sync(String content, java.util.function.Consumer<String> linker, Runnable unlinker) {
        urlExtractor.firstUrl(content).ifPresentOrElse(url -> {
            linker.accept(url);
            eventPublisher.publishEvent(new FeedLinkPreviewRequested());
        }, unlinker);
    }
}
