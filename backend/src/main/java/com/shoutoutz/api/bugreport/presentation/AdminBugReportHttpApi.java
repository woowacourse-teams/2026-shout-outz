package com.shoutoutz.api.bugreport.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.bugreport.application.BugReportAdminService;
import com.shoutoutz.api.bugreport.presentation.dto.request.BugReportAdminFindAllRequest;
import com.shoutoutz.api.bugreport.presentation.dto.request.BugReportStatusUpdateRequest;
import com.shoutoutz.api.bugreport.presentation.dto.response.BugReportAdminDetailResponse;
import com.shoutoutz.api.bugreport.presentation.dto.response.BugReportAdminFindAllResponse;
import com.shoutoutz.api.bugreport.presentation.dto.response.BugReportStatusUpdateResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
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
@RequestMapping("/api/v1/admin/bug-reports")
@RequiredArgsConstructor
public class AdminBugReportHttpApi {

    private final BugReportAdminService bugReportAdminService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<BugReportAdminFindAllResponse.Item>>> findAll(
            @LoginUser AuthenticatedUser loginUser,
            @Valid @ModelAttribute BugReportAdminFindAllRequest request
    ) {
        BugReportAdminFindAllResponse response = bugReportAdminService.findAll(
                loginUser.role(),
                request
        );
        return ResponseEntity.ok(SuccessResponse.success(response.items(), response.meta()));
    }

    @GetMapping("/{bugReportId}")
    public ResponseEntity<SuccessResponse<BugReportAdminDetailResponse>> findDetail(
            @PathVariable long bugReportId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        return ResponseEntity.ok(SuccessResponse.success(
                bugReportAdminService.findDetail(bugReportId, loginUser.role())
        ));
    }

    @PatchMapping("/{bugReportId}/status")
    public ResponseEntity<SuccessResponse<BugReportStatusUpdateResponse>> updateStatus(
            @PathVariable long bugReportId,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody BugReportStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(SuccessResponse.success(bugReportAdminService.updateStatus(
                bugReportId,
                loginUser.userId(),
                loginUser.role(),
                request
        )));
    }
}
