package com.dropit.backend.visit;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteVisitService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final SiteVisitorRepository siteVisitorRepository;

    public SiteVisitService(SiteVisitorRepository siteVisitorRepository) {
        this.siteVisitorRepository = siteVisitorRepository;
    }

    @Transactional
    public SiteVisitResponse record(String visitorId) {
        LocalDate today = LocalDate.now(KOREA_ZONE);
        siteVisitorRepository.insertIfAbsent(visitorId.trim(), today);
        return new SiteVisitResponse(
            siteVisitorRepository.countForDay(today),
            siteVisitorRepository.countTotalVisitors()
        );
    }
}
