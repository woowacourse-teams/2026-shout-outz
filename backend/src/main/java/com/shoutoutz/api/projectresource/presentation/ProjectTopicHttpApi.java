package com.shoutoutz.api.projectresource.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.projectresource.application.ProjectTopicService;
import com.shoutoutz.api.projectresource.presentation.dto.response.ProjectTopicsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 탐색 화면 프로젝트 탭 왼쪽의 기술 주제 목록. 주제를 고르면 프로젝트 목록 조회에 topicId 를 붙여 거른다.
 */
@RestController
@RequiredArgsConstructor
public class ProjectTopicHttpApi {

    private final ProjectTopicService projectTopicService;

    @GetMapping("/api/v1/projects/topics")
    public ResponseEntity<SuccessResponse<ProjectTopicsResponse>> findTopics() {
        return ResponseEntity.ok(SuccessResponse.success(projectTopicService.findTopics()));
    }
}
