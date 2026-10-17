package com.shoutoutz.api.magazine.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.magazine.application.MagazineService;
import com.shoutoutz.api.magazine.application.dto.MagazinePage;
import com.shoutoutz.api.magazine.presentation.dto.request.MagazineSaveRequest;
import com.shoutoutz.api.magazine.presentation.dto.response.MagazineAdminResponse;
import com.shoutoutz.api.magazine.presentation.dto.response.MagazineSummaryResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/magazines")
@RequiredArgsConstructor
public class MagazineAdminHttpApi {

    private final MagazineService magazineService;

    @PostMapping
    public ResponseEntity<SuccessResponse<MagazineAdminResponse>> create(
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody MagazineSaveRequest request
    ) {
        MagazineAdminResponse response = magazineService.create(loginUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<List<MagazineSummaryResponse>>> findAll(
            @LoginUser AuthenticatedUser loginUser,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        MagazinePage page = magazineService.findAllAdmin(loginUser.userId(), cursor, size);
        return ResponseEntity.ok(SuccessResponse.success(page.items(), page.meta()));
    }

    @GetMapping("/{magazineId}")
    public ResponseEntity<SuccessResponse<MagazineAdminResponse>> find(
            @PathVariable long magazineId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        MagazineAdminResponse response = magazineService.findAdmin(magazineId, loginUser.userId());
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @PutMapping("/{magazineId}")
    public ResponseEntity<SuccessResponse<MagazineAdminResponse>> update(
            @PathVariable long magazineId,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody MagazineSaveRequest request
    ) {
        MagazineAdminResponse response = magazineService.update(magazineId, loginUser.userId(), request);
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @PostMapping("/{magazineId}/publish")
    public ResponseEntity<SuccessResponse<MagazineAdminResponse>> publish(
            @PathVariable long magazineId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        MagazineAdminResponse response = magazineService.publish(magazineId, loginUser.userId());
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @PostMapping("/{magazineId}/unpublish")
    public ResponseEntity<SuccessResponse<MagazineAdminResponse>> unpublish(
            @PathVariable long magazineId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        MagazineAdminResponse response = magazineService.unpublish(magazineId, loginUser.userId());
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{magazineId}")
    public ResponseEntity<Void> delete(
            @PathVariable long magazineId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        magazineService.delete(magazineId, loginUser.userId());
        return ResponseEntity.noContent().build();
    }
}
