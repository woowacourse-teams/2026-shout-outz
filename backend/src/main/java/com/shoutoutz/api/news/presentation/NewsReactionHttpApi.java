package com.shoutoutz.api.news.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.news.application.NewsReactionService;
import com.shoutoutz.api.news.presentation.dto.response.NewsReactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/news/{newsId}/reactions")
@RequiredArgsConstructor
public class NewsReactionHttpApi {

    private final NewsReactionService newsReactionService;

    @PutMapping("/{type}")
    public ResponseEntity<SuccessResponse<NewsReactionResponse>> add(
            @PathVariable long newsId,
            @PathVariable String type,
            @LoginUser AuthenticatedUser user
    ) {
        NewsReactionResponse response = newsReactionService.add(newsId, user.userId(), type);
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{type}")
    public ResponseEntity<SuccessResponse<NewsReactionResponse>> remove(
            @PathVariable long newsId,
            @PathVariable String type,
            @LoginUser AuthenticatedUser user
    ) {
        NewsReactionResponse response = newsReactionService.remove(newsId, user.userId(), type);
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
