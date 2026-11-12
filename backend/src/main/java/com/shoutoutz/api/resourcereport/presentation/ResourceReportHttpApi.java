package com.shoutoutz.api.resourcereport.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.resourcereport.application.ResourceReportService;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceAdditionReportRequest;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceRemovalReportRequest;
import com.shoutoutz.api.resourcereport.presentation.dto.response.MyResourceReportResponse;
import com.shoutoutz.api.resourcereport.presentation.dto.response.ResourceAdditionReportResponse;
import com.shoutoutz.api.resourcereport.presentation.dto.response.ResourceRemovalReportResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프로젝트 기술 이야기 자료 제보. 로그인해야 하고, 제보는 그 프로젝트 팀원만 보낼 수 있다.
 */
@RestController
@RequestMapping("/api/v1/projects/@{slug}/resource-reports")
@RequiredArgsConstructor
public class ResourceReportHttpApi {

    private final ResourceReportService resourceReportService;

    @PostMapping("/removals")
    public ResponseEntity<SuccessResponse<List<ResourceRemovalReportResponse>>> reportRemovals(
            @PathVariable String slug,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody ResourceRemovalReportRequest request
    ) {
        List<ResourceRemovalReportResponse> response = resourceReportService.reportRemovals(
                slug,
                loginUser.userId(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.success(response));
    }

    @PostMapping("/additions")
    public ResponseEntity<SuccessResponse<ResourceAdditionReportResponse>> reportAddition(
            @PathVariable String slug,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody ResourceAdditionReportRequest request
    ) {
        ResourceAdditionReportResponse response = resourceReportService.reportAddition(
                slug,
                loginUser.userId(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<SuccessResponse<List<MyResourceReportResponse>>> findMine(
            @PathVariable String slug,
            @LoginUser AuthenticatedUser loginUser
    ) {
        return ResponseEntity.ok(SuccessResponse.success(
                resourceReportService.findMine(slug, loginUser.userId())
        ));
    }
}
