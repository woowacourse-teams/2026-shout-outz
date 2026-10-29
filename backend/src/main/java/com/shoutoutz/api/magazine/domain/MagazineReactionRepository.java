package com.shoutoutz.api.magazine.domain;

import java.util.List;
import java.util.Map;

public interface MagazineReactionRepository {

    void add(long magazineId, long userId, MagazineReactionType type);

    boolean remove(long magazineId, long userId, MagazineReactionType type);

    long countByMagazineId(long magazineId);

    MagazineReactionCounts findCountsByMagazineId(long magazineId, Long viewerId);

    Map<Long, MagazineReactionCounts> findCountsByMagazineIds(List<Long> magazineIds, Long viewerId);
}
