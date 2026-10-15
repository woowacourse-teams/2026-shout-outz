package com.shoutoutz.api.interview.domain;

import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.time.Instant;
import java.time.LocalDate;

public interface InterviewViewRepository {
    long record(long interviewId, VisitorKey visitorKey, LocalDate viewedOn, Instant viewedAt);
}
