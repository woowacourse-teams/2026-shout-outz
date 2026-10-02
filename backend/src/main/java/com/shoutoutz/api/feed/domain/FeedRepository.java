package com.shoutoutz.api.feed.domain;

import java.util.List;
import java.util.Optional;

public interface FeedRepository {

    Feed save(Feed feed);

    Feed update(Feed feed);

    Optional<Feed> findActiveById(long feedId);

    void saveCategories(long feedId, List<Long> categoryIds);

    void saveMedia(long feedId, List<Long> mediaIds);
}
