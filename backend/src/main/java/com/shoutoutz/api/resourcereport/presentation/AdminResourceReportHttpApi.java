package com.shoutoutz.api.resourcereport.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.resourcereport.application.ResourceReportAdminService;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceReportAdminFindAllRequest;
import com.shoutoutz.api.resourcereport.presentation.dto.request.ResourceReportStatusUpdateRequest;
import com.shoutoutz.api.resourcereport.presentation.dto.response.ResourceReportAdminFindAllResponse;
import com.shoutoutz.api.resourcereport.presentation.dto.response.ResourceReportStatusUpdateResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/resource-reports")
@RequiredArgsConstructor
public class AdminResourceReportHttpApi {

    private final ResourceReportAdminService resourceReportAdminService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<ResourceReportAdminFindAllResponse.Item>>> findAll(
            @LoginUser AuthenticatedUser loginUser,
            @Valid @ModelAttribute ResourceReportAdminFindAllRequest request
    ) {
        ResourceReportAdminFindAllResponse response = resourceReportAdminService.findAll(
                loginUser.role(),
                request
        );
        return ResponseEntity.ok(SuccessResponse.success(response.items(), response.meta()));
    }

    @PatchMapping("/{reportId}/status")
    public ResponseEntity<SuccessResponse<ResourceReportStatusUpdateResponse>> updateStatus(
            @PathVariable long reportId,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody ResourceReportStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(SuccessResponse.success(resourceReportAdminService.updateStatus(
                reportId,
                loginUser.userId(),
                loginUser.role(),
                request
        )));
    }
}
