package com.shoutoutz.api.bugreport.application;

import com.shoutoutz.api.bugreport.domain.BugReport;
import com.shoutoutz.api.bugreport.domain.BugReportRepository;
import com.shoutoutz.api.bugreport.presentation.dto.request.BugReportCreateRequest;
import com.shoutoutz.api.bugreport.presentation.dto.response.BugReportCreateResponse;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BugReportService {

    private final BugReportRepository bugReportRepository;
    private final Clock clock;

    @Transactional
    public BugReportCreateResponse create(Long reporterUserId, BugReportCreateRequest request) {
        BugReport bugReport = BugReport.create(request.content(), reporterUserId, clock.instant());
        return BugReportCreateResponse.from(bugReportRepository.save(bugReport));
    }
}
