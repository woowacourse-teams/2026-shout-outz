package com.shoutoutz.api.home.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.home.application.HomeStatisticsService;
import com.shoutoutz.api.home.presentation.dto.response.HomeStatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home/statistics")
@RequiredArgsConstructor
public class HomeStatisticsHttpApi {

    private final HomeStatisticsService homeStatisticsService;

    @GetMapping
    public ResponseEntity<SuccessResponse<HomeStatisticsResponse>> find() {
        return ResponseEntity.ok(SuccessResponse.success(homeStatisticsService.find()));
    }
}
