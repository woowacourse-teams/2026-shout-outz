package com.shoutoutz.api.project.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.project.application.ProjectViewService;
import com.shoutoutz.api.project.presentation.dto.response.ProjectViewRecordResponse;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import com.shoutoutz.api.visitor.presentation.Visitor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectViewHttpApi {

    private final ProjectViewService projectViewService;

    /**
     * 로그인하지 않아도 기록한다. 같은 날 다시 조회해도 200과 현재 조회수를 준다.
     */
    @PostMapping("/{projectId}/views")
    public ResponseEntity<SuccessResponse<ProjectViewRecordResponse>> record(
            @PathVariable long projectId,
            @Visitor VisitorKey visitorKey
    ) {
        ProjectViewRecordResponse response = projectViewService.record(projectId, visitorKey);
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
