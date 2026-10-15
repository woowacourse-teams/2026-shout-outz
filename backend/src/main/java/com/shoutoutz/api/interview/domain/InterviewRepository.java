package com.shoutoutz.api.interview.domain;

import java.util.Optional;

public interface InterviewRepository {
    Interview save(Interview interview);
    Optional<Interview> findActiveById(long id);
    Optional<Interview> findForUpdate(long id);
}
