package com.shoutoutz.api.user.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.user.application.UserCommandService;
import com.shoutoutz.api.user.application.UserQueryService;
import com.shoutoutz.api.user.application.dto.result.UserProfileUpdateResult;
import com.shoutoutz.api.user.application.dto.result.UserProfileResult;
import com.shoutoutz.api.user.application.dto.result.UserProfileSummaryResult;
import com.shoutoutz.api.user.application.dto.result.UserSearchResult;
import com.shoutoutz.api.user.presentation.dto.request.UserProfileUpdateRequest;
import com.shoutoutz.api.user.presentation.dto.request.UserSearchRequest;
import com.shoutoutz.api.user.presentation.dto.response.UserProfileResponse;
import com.shoutoutz.api.user.presentation.dto.response.UserProfileSummaryResponse;
import com.shoutoutz.api.user.presentation.dto.response.UserProfileUpdateResponse;
import com.shoutoutz.api.user.presentation.dto.response.UserSearchMetaResponse;
import com.shoutoutz.api.user.presentation.dto.response.UserSearchResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserHttpApi {

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;

    @GetMapping("/me/summary")
    public ResponseEntity<SuccessResponse<UserProfileSummaryResponse>> getMyProfileSummary(
            @LoginUser AuthenticatedUser authenticatedUser
    ) {
        UserProfileSummaryResult result = userQueryService.getMyProfileSummary(
                authenticatedUser.userId()
        );
        UserProfileSummaryResponse response = UserProfileSummaryResponse.from(result);

        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<SuccessResponse<UserProfileResponse>> getMyProfile(
            @LoginUser AuthenticatedUser authenticatedUser
    ) {
        UserProfileResult result = userQueryService.getMyProfile(authenticatedUser.userId());
        UserProfileResponse response = UserProfileResponse.from(result);

        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @PutMapping("/me")
    public ResponseEntity<SuccessResponse<UserProfileUpdateResponse>> updateMyProfile(
            @LoginUser AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        UserProfileUpdateResult result = userCommandService.updateMyProfile(
                request.toCommand(authenticatedUser.userId())
        );
        UserProfileUpdateResponse response = UserProfileUpdateResponse.from(result);

        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @GetMapping("/search")
    public ResponseEntity<SuccessResponse<UserSearchResponse>> searchProjectMember(
            @LoginUser AuthenticatedUser authenticatedUser,
            @Valid @ModelAttribute UserSearchRequest request
    ) {
        UserSearchResult result = userQueryService.searchProjectMember(
                authenticatedUser.userId(),
                request.keyword(),
                request.cursor(),
                request.resolvedSize()
        );
        UserSearchResponse response = UserSearchResponse.from(result);
        UserSearchMetaResponse meta = UserSearchMetaResponse.from(result);

        return ResponseEntity.ok(SuccessResponse.success(response, meta));
    }

    @GetMapping("/{handle}")
    public ResponseEntity<SuccessResponse<UserProfileResponse>> getPublicProfile(
            @Pattern(
                    regexp = "^[A-Za-z0-9_-]{2,30}$",
                    message = "handle 형식이 올바르지 않습니다."
            )
            @PathVariable String handle
    ) {
        UserProfileResult result = userQueryService.getPublicProfile(handle);
        UserProfileResponse response = UserProfileResponse.from(result);

        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
