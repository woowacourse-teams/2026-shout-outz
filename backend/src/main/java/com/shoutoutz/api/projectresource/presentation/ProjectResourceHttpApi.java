package com.shoutoutz.api.projectresource.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.projectresource.application.ProjectResourceService;
import com.shoutoutz.api.projectresource.presentation.dto.request.ProjectResourceTopicsRequest;
import com.shoutoutz.api.projectresource.presentation.dto.request.ProjectTopicResourcesRequest;
import com.shoutoutz.api.projectresource.presentation.dto.response.ProjectResourceTopicsResponse;
import com.shoutoutz.api.projectresource.presentation.dto.response.ProjectTopicResourcesResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프로젝트 상세 기술 이야기 탭. 탭을 열거나 직군을 바꾸면 주제 목록을, 주제를 고르면 주제별 자료를 부른다.
 */
@RestController
@RequestMapping("/api/v1/projects/@{slug}")
@RequiredArgsConstructor
public class ProjectResourceHttpApi {

    private final ProjectResourceService projectResourceService;

    @GetMapping("/resource-topics")
    public ResponseEntity<SuccessResponse<ProjectResourceTopicsResponse>> findTopics(
            @PathVariable String slug,
            @Valid @ModelAttribute ProjectResourceTopicsRequest request
    ) {
        return ResponseEntity.ok(SuccessResponse.success(
                projectResourceService.findTopics(slug, request.resolvedJobRole())
        ));
    }

    @GetMapping("/resources")
    public ResponseEntity<SuccessResponse<ProjectTopicResourcesResponse>> findResources(
            @PathVariable String slug,
            @Valid @ModelAttribute ProjectTopicResourcesRequest request
    ) {
        return ResponseEntity.ok(SuccessResponse.success(
                projectResourceService.findResources(slug, request.topicId(), request.resolvedJobRole())
        ));
    }
}
