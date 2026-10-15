package com.shoutoutz.api.interview.domain;

import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public interface InterviewRepository {
    Interview save(Interview interview);
    Optional<Interview> findActiveById(long id);
    Optional<Interview> findForUpdate(long id);
    long recordView(long id, VisitorKey visitorKey, LocalDate viewedOn, Instant viewedAt);
}
