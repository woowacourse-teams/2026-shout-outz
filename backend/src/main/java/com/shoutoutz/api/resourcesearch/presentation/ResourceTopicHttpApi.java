package com.shoutoutz.api.resourcesearch.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.resourcesearch.application.ResourceTopicQueryService;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceTopicPopularRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.PopularTopicResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceTopicSideResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resources/topics")
@RequiredArgsConstructor
public class ResourceTopicHttpApi {

    private final ResourceTopicQueryService resourceTopicQueryService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<ResourceTopicSideResponse>>> findAllBySide() {
        return ResponseEntity.ok(SuccessResponse.success(resourceTopicQueryService.findAllBySide()));
    }

    @GetMapping("/popular")
    public ResponseEntity<SuccessResponse<List<PopularTopicResponse>>> findPopular(
            @Valid @ModelAttribute ResourceTopicPopularRequest request
    ) {
        return ResponseEntity.ok(SuccessResponse.success(resourceTopicQueryService.findPopular(request)));
    }
}
