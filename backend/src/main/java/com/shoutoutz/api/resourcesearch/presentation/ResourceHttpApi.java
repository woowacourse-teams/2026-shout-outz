package com.shoutoutz.api.resourcesearch.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.resourcesearch.application.ResourceQueryService;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceSearchRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceSearchResponse;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import com.shoutoutz.api.visitor.presentation.Visitor;
import jakarta.validation.Valid;
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
}
