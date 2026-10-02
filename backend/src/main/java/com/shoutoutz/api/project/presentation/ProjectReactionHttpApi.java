package com.shoutoutz.api.project.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.project.application.ProjectReactionService;
import com.shoutoutz.api.project.presentation.dto.response.ProjectReactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/@{slug}/reactions")
@RequiredArgsConstructor
public class ProjectReactionHttpApi {

    private final ProjectReactionService projectReactionService;

    @PutMapping("/{type}")
    public ResponseEntity<SuccessResponse<ProjectReactionResponse>> add(
            @PathVariable String slug,
            @PathVariable String type,
            @LoginUser AuthenticatedUser user
    ) {
        ProjectReactionResponse response = projectReactionService.add(slug, user.userId(), type);
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{type}")
    public ResponseEntity<SuccessResponse<ProjectReactionResponse>> remove(
            @PathVariable String slug,
            @PathVariable String type,
            @LoginUser AuthenticatedUser user
    ) {
        ProjectReactionResponse response = projectReactionService.remove(slug, user.userId(), type);
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
