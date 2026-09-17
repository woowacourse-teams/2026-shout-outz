package com.shoutoutz.api.project.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.project.application.ProjectService;
import com.shoutoutz.api.project.presentation.dto.request.UserProjectFindRequest;
import com.shoutoutz.api.project.presentation.dto.response.UserProjectFindResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 페이지의 프로젝트 목록 API.
 */
@RestController
@RequestMapping("/api/v1/users/{handle}/projects")
@RequiredArgsConstructor
@Validated
public class UserProjectHttpApi {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<UserProjectFindResponse.Item>>> findAll(
            @Pattern(
                    regexp = "^[A-Za-z0-9_-]{2,30}$",
                    message = "handle 형식이 올바르지 않습니다."
            )
            @PathVariable String handle,
            @Valid @ModelAttribute UserProjectFindRequest request
    ) {
        UserProjectFindResponse response = projectService.findAllByUser(handle, request);
        return ResponseEntity.ok(SuccessResponse.success(response.projects(), response.meta()));
    }
}
