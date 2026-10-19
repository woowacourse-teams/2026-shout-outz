package com.shoutoutz.api.comment.domain;

import java.util.Optional;

public interface MagazineCommentRepository {

    MagazineComment save(MagazineComment comment);

    Optional<MagazineComment> findById(long commentId);
}
