package com.shoutoutz.api.project.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.project.application.ProjectService;
import com.shoutoutz.api.project.presentation.dto.request.ProjectCreateRequest;
import com.shoutoutz.api.project.presentation.dto.request.ProjectFilterOptionsRequest;
import com.shoutoutz.api.project.presentation.dto.request.ProjectFindAllRequest;
import com.shoutoutz.api.project.presentation.dto.request.ProjectUpdateRequest;
import com.shoutoutz.api.project.presentation.dto.response.ProjectCreateResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectDeleteResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectDetailResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectFilterOptionsResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectFindAllResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectRestoreResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectUpdateResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectHttpApi {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<SuccessResponse<ProjectCreateResponse>> create(
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody ProjectCreateRequest request
    ) {
        ProjectCreateResponse response = projectService.create(loginUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
    }

    /**
     * 작성자만 수정할 수 있다. 반려된 프로젝트를 수정하면 재심사 요청으로 처리한다.
     */
    @PutMapping("/{projectId}")
    public ResponseEntity<SuccessResponse<ProjectUpdateResponse>> update(
            @LoginUser AuthenticatedUser loginUser,
            @PathVariable long projectId,
            @Valid @RequestBody ProjectUpdateRequest request
    ) {
        ProjectUpdateResponse response = projectService.update(projectId, loginUser.userId(), request);
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<List<ProjectFindAllResponse.Item>>> findAll(
            @Valid @ModelAttribute ProjectFindAllRequest request
    ) {
        ProjectFindAllResponse response = projectService.findAll(request);
        return ResponseEntity.ok(SuccessResponse.success(response.items(), response.meta()));
    }

    /**
     * 비로그인도 조회할 수 있다. 목록 조회에 적용 중인 검색어와 모달에서 고른 필터로 선택지별 프로젝트 수를 센다.
     */
    @GetMapping("/filters")
    public ResponseEntity<SuccessResponse<ProjectFilterOptionsResponse>> findFilterOptions(
            @Valid @ModelAttribute ProjectFilterOptionsRequest request
    ) {
        return ResponseEntity.ok(SuccessResponse.success(projectService.findFilterOptions(request)));
    }

    /**
     * 비로그인도 조회할 수 있다. 로그인한 경우에는 본인 프로젝트 조회 권한과 리액션 여부 판단에 사용한다.
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<SuccessResponse<ProjectDetailResponse>> findDetail(
            @LoginUser(required = false) AuthenticatedUser loginUser,
            @PathVariable long projectId
    ) {
        ProjectDetailResponse response = projectService.findDetail(
                projectId,
                AuthenticatedUser.userIdOrNull(loginUser)
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    /**
     * 등록자 본인만 삭제할 수 있다. 심사 중인 프로젝트도 삭제할 수 있다.
     */
    @DeleteMapping("/{projectId}")
    public ResponseEntity<SuccessResponse<ProjectDeleteResponse>> delete(
            @LoginUser AuthenticatedUser loginUser,
            @PathVariable long projectId
    ) {
        ProjectDeleteResponse response = ProjectDeleteResponse.from(
                projectService.delete(projectId, loginUser.userId())
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    /**
     * 등록자 본인만 복구 기한 안에 복구할 수 있다. 승인 상태는 삭제 이전 값을 그대로 유지한다.
     */
    @PostMapping("/{projectId}/restore")
    public ResponseEntity<SuccessResponse<ProjectRestoreResponse>> restore(
            @LoginUser AuthenticatedUser loginUser,
            @PathVariable long projectId
    ) {
        ProjectRestoreResponse response = ProjectRestoreResponse.from(
                projectService.restore(projectId, loginUser.userId())
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
