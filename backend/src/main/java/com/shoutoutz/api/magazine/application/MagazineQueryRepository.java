package com.shoutoutz.api.magazine.application;

import com.shoutoutz.api.magazine.application.dto.MagazineCursor;
import com.shoutoutz.api.magazine.application.dto.MagazineItem;
import java.util.List;
import java.util.Optional;

public interface MagazineQueryRepository {
    List<MagazineItem> findPage(MagazineCursor cursor, int limit, boolean admin);
    long count(boolean admin);
    Optional<MagazineItem> findPublished(long id);
}
