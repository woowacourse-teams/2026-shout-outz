package com.shoutoutz.api.post.domain;

import java.util.List;
import java.util.Optional;

public interface PostRepository {

    Post save(Post post);

    Post update(Post post);

    Optional<Post> findActiveById(long postId);

    void saveCategories(long postId, List<Long> categoryIds);

    void saveMedia(long postId, List<Long> mediaIds);
}
