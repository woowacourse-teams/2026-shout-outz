package com.shoutoutz.api.news.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.news.application.NewsService;
import com.shoutoutz.api.news.presentation.dto.request.NoticeCreateRequest;
import com.shoutoutz.api.news.presentation.dto.response.NoticeCreateResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsHttpApi {

    private final NewsService newsService;

    // TODO: 인증 객체 ROLE 조회 필요
    @PostMapping("/notices")
    public ResponseEntity<SuccessResponse<NoticeCreateResponse>> createNotice(
            @Valid @RequestBody NoticeCreateRequest body
    ) {
        NoticeCreateResponse response = newsService.createNotice(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
    }
}
