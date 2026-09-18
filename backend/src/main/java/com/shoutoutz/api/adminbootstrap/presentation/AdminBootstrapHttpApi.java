package com.shoutoutz.api.adminbootstrap.presentation;

import com.shoutoutz.api.adminbootstrap.application.AdminBootstrapResult;
import com.shoutoutz.api.adminbootstrap.application.AdminBootstrapService;
import com.shoutoutz.api.adminbootstrap.presentation.dto.AdminBootstrapRequest;
import com.shoutoutz.api.adminbootstrap.presentation.dto.AdminBootstrapResponse;
import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.auth.presentation.session.AuthSessionManager;
import com.shoutoutz.api.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminBootstrapHttpApi {

    private final AdminBootstrapService adminBootstrapService;
    private final AuthSessionManager authSessionManager;

    @PostMapping("/api/v1/auth/admin/bootstrap")
    public ResponseEntity<SuccessResponse<AdminBootstrapResponse>> bootstrap(
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody AdminBootstrapRequest request,
            HttpServletRequest httpRequest
    ) {
        AdminBootstrapResult result = adminBootstrapService.promote(
                loginUser.userId(),
                request.code()
        );
        authSessionManager.establishAuthenticatedSession(
                httpRequest,
                result.userId(),
                result.role()
        );

        return ResponseEntity.ok(
                SuccessResponse.success(AdminBootstrapResponse.from(result))
        );
    }
}
