package com.shoutoutz.api.magazine.domain;

import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public interface MagazineRepository {
    Magazine save(Magazine magazine);
    Optional<Magazine> findActiveById(long id);
    Optional<Magazine> findForUpdate(long id);
    long recordView(long id, VisitorKey visitorKey, LocalDate viewedOn, Instant viewedAt);
}
