package com.shoutoutz.api.homebanner.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.homebanner.application.HomeBannerAdminService;
import com.shoutoutz.api.homebanner.presentation.dto.request.HomeBannerUpsertRequest;
import com.shoutoutz.api.homebanner.presentation.dto.response.HomeBannerAdminResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/home/banners")
@RequiredArgsConstructor
public class HomeBannerAdminHttpApi {

    private final HomeBannerAdminService homeBannerAdminService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<HomeBannerAdminResponse>>> findAll(
            @LoginUser AuthenticatedUser user
    ) {
        List<HomeBannerAdminResponse> response = homeBannerAdminService.findAll(user.role());
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<HomeBannerAdminResponse>> save(
            @LoginUser AuthenticatedUser user,
            @Valid @RequestBody HomeBannerUpsertRequest request
    ) {
        HomeBannerAdminResponse response = homeBannerAdminService.save(
                user.userId(),
                user.role(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.success(response));
    }

    @PutMapping("/{bannerId}")
    public ResponseEntity<SuccessResponse<HomeBannerAdminResponse>> update(
            @PathVariable long bannerId,
            @LoginUser AuthenticatedUser user,
            @Valid @RequestBody HomeBannerUpsertRequest request
    ) {
        HomeBannerAdminResponse response = homeBannerAdminService.update(
                bannerId,
                user.role(),
                request
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{bannerId}")
    public ResponseEntity<Void> delete(
            @PathVariable long bannerId,
            @LoginUser AuthenticatedUser user
    ) {
        homeBannerAdminService.delete(bannerId, user.role());
        return ResponseEntity.noContent().build();
    }
}
