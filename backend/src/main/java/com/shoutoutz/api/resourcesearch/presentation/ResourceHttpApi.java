package com.shoutoutz.api.resourcesearch.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.resourcesearch.application.ResourceQueryService;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceRecentRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceSearchRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.PopularKeywordResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceCardResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceSearchResponse;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import com.shoutoutz.api.visitor.presentation.Visitor;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceHttpApi {

    private final ResourceQueryService resourceQueryService;

    @GetMapping("/search")
    public ResponseEntity<SuccessResponse<ResourceSearchResponse>> search(
            @Valid @ModelAttribute ResourceSearchRequest request,
            @Visitor VisitorKey visitorKey
    ) {
        return ResponseEntity.ok(SuccessResponse.success(resourceQueryService.search(request, visitorKey)));
    }

    @GetMapping("/recent")
    public ResponseEntity<SuccessResponse<List<ResourceCardResponse>>> findRecent(
            @Valid @ModelAttribute ResourceRecentRequest request
    ) {
        return ResponseEntity.ok(SuccessResponse.success(resourceQueryService.findRecent(request)));
    }

    @GetMapping("/popular-keywords")
    public ResponseEntity<SuccessResponse<List<PopularKeywordResponse>>> findPopularKeywords() {
        return ResponseEntity.ok(SuccessResponse.success(resourceQueryService.findPopularKeywords()));
    }
}
