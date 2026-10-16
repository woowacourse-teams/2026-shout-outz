package com.shoutoutz.api.interview.application;

import com.shoutoutz.api.interview.application.dto.InterviewCursor;
import com.shoutoutz.api.interview.application.dto.InterviewItem;
import java.util.List;
import java.util.Optional;

public interface InterviewQueryRepository {
    List<InterviewItem> findPage(InterviewCursor cursor, int limit, boolean admin);
    long count(boolean admin);
    Optional<InterviewItem> findPublished(long id);
}
