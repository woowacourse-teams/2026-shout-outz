package com.shoutoutz.api.magazine.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.magazine.application.MagazineService;
import com.shoutoutz.api.magazine.application.dto.MagazinePage;
import com.shoutoutz.api.magazine.presentation.dto.response.MagazineDetailResponse;
import com.shoutoutz.api.magazine.presentation.dto.response.MagazineSummaryResponse;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import com.shoutoutz.api.visitor.presentation.Visitor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/magazines")
@RequiredArgsConstructor
public class MagazineHttpApi {

    private final MagazineService magazineService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<MagazineSummaryResponse>>> findAll(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @LoginUser(required = false) AuthenticatedUser loginUser
    ) {
        MagazinePage page = magazineService.findAll(cursor, size, AuthenticatedUser.userIdOrNull(loginUser));
        return ResponseEntity.ok(SuccessResponse.success(page.items(), page.meta()));
    }

    @GetMapping("/{magazineId}")
    public ResponseEntity<SuccessResponse<MagazineDetailResponse>> find(
            @PathVariable long magazineId,
            @LoginUser(required = false) AuthenticatedUser loginUser,
            @Visitor VisitorKey visitorKey
    ) {
        MagazineDetailResponse response = magazineService.find(
                magazineId, AuthenticatedUser.userIdOrNull(loginUser), visitorKey);
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
