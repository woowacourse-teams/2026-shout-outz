package com.shoutoutz.api.homebanner.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.homebanner.application.HomeBannerService;
import com.shoutoutz.api.homebanner.presentation.dto.response.HomeBannerResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home/banners")
@RequiredArgsConstructor
public class HomeBannerHttpApi {

    private final HomeBannerService homeBannerService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<HomeBannerResponse>>> findAll() {
        List<HomeBannerResponse> response = homeBannerService.findAll();
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
