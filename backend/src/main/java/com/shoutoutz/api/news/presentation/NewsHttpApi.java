package com.shoutoutz.api.news.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.news.application.NewsService;
import com.shoutoutz.api.news.presentation.dto.request.EventCreateRequest;
import com.shoutoutz.api.news.presentation.dto.request.NewsFindAllRequest;
import com.shoutoutz.api.news.presentation.dto.request.NewsFindRequest;
import com.shoutoutz.api.news.presentation.dto.request.NoticeCreateRequest;
import com.shoutoutz.api.news.presentation.dto.request.NewsUpdateRequest;
import com.shoutoutz.api.news.presentation.dto.response.EventCreateResponse;
import com.shoutoutz.api.news.presentation.dto.response.NewsDeleteResponse;
import com.shoutoutz.api.news.presentation.dto.response.NewsFindAllResponse;
import com.shoutoutz.api.news.presentation.dto.response.NewsFindResponse;
import com.shoutoutz.api.news.presentation.dto.response.NoticeCreateResponse;
import com.shoutoutz.api.news.presentation.dto.response.NewsUpdateResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
@Validated
public class NewsHttpApi {

    private final NewsService newsService;

    @PostMapping("/notices")
    public ResponseEntity<SuccessResponse<NoticeCreateResponse>> createNotice(
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody NoticeCreateRequest request
    ) {
        NoticeCreateResponse response = newsService.createNotice(
                loginUser.userId(),
                loginUser.role(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
    }

    @PostMapping("/events")
    public ResponseEntity<SuccessResponse<EventCreateResponse>> createEvent(
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody EventCreateRequest request
    ) {
        EventCreateResponse response = newsService.createEvent(
                loginUser.userId(),
                loginUser.role(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<List<NewsFindAllResponse.Item>>> findAll(
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(required = false) String eventStatus,
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cursor
    ) {
        NewsFindAllResponse response = newsService.findAll(
                new NewsFindAllRequest(type, eventStatus, sort, size, cursor));
        return ResponseEntity.ok(SuccessResponse.success(response.items(), response.meta()));
    }

    /**
     * 소식(News) 상세 단건 조회
     */
    @GetMapping("/{newsId}")
    public ResponseEntity<SuccessResponse<NewsFindResponse>> findDetail(
            @PathVariable long newsId,
            @RequestParam(defaultValue = "true") boolean navigation
    ) {
        NewsFindRequest request = new NewsFindRequest(newsId, navigation);
        NewsFindResponse response = newsService.findDetail(request);
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @PutMapping("/{newsId}")
    public ResponseEntity<SuccessResponse<NewsUpdateResponse>> update(
            @LoginUser AuthenticatedUser loginUser,
            @PathVariable long newsId,
            @Valid @RequestBody NewsUpdateRequest request
    ) {
        NewsUpdateResponse response = newsService.update(newsId, loginUser.role(), request);
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{newsId}")
    public ResponseEntity<SuccessResponse<NewsDeleteResponse>> delete(
            @LoginUser AuthenticatedUser loginUser,
            @PathVariable long newsId
    ) {
        NewsDeleteResponse response = newsService.delete(
                newsId,
                loginUser.role()
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
