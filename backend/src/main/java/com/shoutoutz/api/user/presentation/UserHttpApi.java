package com.shoutoutz.api.user.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.user.application.UserQueryService;
import com.shoutoutz.api.user.application.dto.result.UserProfileResult;
import com.shoutoutz.api.user.application.dto.result.UserProfileSummaryResult;
import com.shoutoutz.api.user.application.dto.result.UserSearchResult;
import com.shoutoutz.api.user.presentation.dto.response.UserProfileResponse;
import com.shoutoutz.api.user.presentation.dto.response.UserProfileSummaryResponse;
import com.shoutoutz.api.user.presentation.dto.response.UserSearchMetaResponse;
import com.shoutoutz.api.user.presentation.dto.response.UserSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserHttpApi {

    private final UserQueryService userQueryService;

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

    @GetMapping("/search")
    public ResponseEntity<SuccessResponse<UserSearchResponse>> searchCrew(
            @LoginUser AuthenticatedUser authenticatedUser,
            @RequestParam String keyword,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        UserSearchResult result = userQueryService.searchCrew(
                authenticatedUser.userId(),
                keyword,
                cursor,
                size
        );
        UserSearchResponse response = UserSearchResponse.from(result);
        UserSearchMetaResponse meta = UserSearchMetaResponse.from(result);

        return ResponseEntity.ok(SuccessResponse.success(response, meta));
    }

    @GetMapping("/{handle}")
    public ResponseEntity<SuccessResponse<UserProfileResponse>> getPublicProfile(
            @PathVariable String handle
    ) {
        UserProfileResult result = userQueryService.getPublicProfile(handle);
        UserProfileResponse response = UserProfileResponse.from(result);

        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
