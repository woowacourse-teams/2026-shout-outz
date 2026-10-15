package com.shoutoutz.api.interview.application;

import com.shoutoutz.api.interview.domain.InterviewViewRepository;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewViewRecordResponse;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewViewService {
    private static final ZoneId VIEW_DATE_ZONE = ZoneId.of("Asia/Seoul");
    private final InterviewAccessService accessService;
    private final InterviewViewRepository repository;
    private final Clock clock;

    @Transactional
    public InterviewViewRecordResponse record(long interviewId, VisitorKey visitorKey) {
        accessService.lockPublished(interviewId);
        Instant viewedAt = clock.instant();
        LocalDate viewedOn = LocalDate.ofInstant(viewedAt, VIEW_DATE_ZONE);
        return new InterviewViewRecordResponse(repository.record(interviewId, visitorKey, viewedOn, viewedAt));
    }
}
