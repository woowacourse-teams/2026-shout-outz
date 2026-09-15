package com.shoutoutz.api.post.application;

import com.shoutoutz.api.post.application.dto.PostCursor;
import com.shoutoutz.api.post.application.dto.PostItem;
import com.shoutoutz.api.post.application.dto.PostMediaReference;
import com.shoutoutz.api.post.application.dto.PostSort;
import java.util.List;
import java.util.Optional;

/**
 * 포스트와 연관 데이터 조회 포트
 */
public interface PostQueryRepository {

    Optional<PostItem> findById(long postId);

    List<PostItem> findAll(PostSort sort, Long categoryId, PostCursor cursor, int limit);

    List<PostMediaReference> findAllMediaByIds(List<Long> mediaIds);
}
