package com.shoutoutz.api.feed.application;

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
        urlExtractor.firstUrl(content).ifPresentOrElse(url -> {
            repository.link(feedId, url);
            eventPublisher.publishEvent(new FeedLinkPreviewRequested());
        }, () -> repository.unlink(feedId));
    }

    public void unlink(long feedId) {
        repository.unlink(feedId);
    }
}
