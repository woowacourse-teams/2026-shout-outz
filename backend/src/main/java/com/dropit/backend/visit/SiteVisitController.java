package com.dropit.backend.visit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/site-visits")
@Tag(name = "Misc", description = "사이트 방문 통계")
public class SiteVisitController {

    private final SiteVisitService siteVisitService;

    public SiteVisitController(SiteVisitService siteVisitService) {
        this.siteVisitService = siteVisitService;
    }

    @PostMapping
    @Operation(summary = "사이트 방문 기록")
    public SiteVisitResponse record(@Valid @RequestBody SiteVisitRequest request) {
        return siteVisitService.record(request.visitorId());
    }
}
