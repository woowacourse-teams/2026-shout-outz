package com.shoutoutz.api.interview.domain;

import java.util.List;
import java.util.Map;

public interface InterviewReactionRepository {

    void add(long interviewId, long userId, InterviewReactionType type);

    boolean remove(long interviewId, long userId, InterviewReactionType type);

    long countByInterviewId(long interviewId);

    InterviewReactionCounts findCountsByInterviewId(long interviewId, Long viewerId);

    Map<Long, InterviewReactionCounts> findCountsByInterviewIds(List<Long> interviewIds, Long viewerId);
}
