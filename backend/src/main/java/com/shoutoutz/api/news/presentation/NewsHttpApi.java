package com.shoutoutz.api.news.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.news.application.NewsService;
import com.shoutoutz.api.news.application.command.CreateEventCommand;
import com.shoutoutz.api.news.application.command.CreateNoticeCommand;
import com.shoutoutz.api.news.application.dto.result.CreateEventResult;
import com.shoutoutz.api.news.application.dto.result.CreateNoticeResult;
import com.shoutoutz.api.news.application.dto.result.NewsFindAllResult;
import com.shoutoutz.api.news.presentation.dto.request.EventCreateRequest;
import com.shoutoutz.api.news.presentation.dto.request.NewsFindAllRequest;
import com.shoutoutz.api.news.presentation.dto.request.NoticeCreateRequest;
import com.shoutoutz.api.news.presentation.dto.response.EventCreateResponse;
import com.shoutoutz.api.news.presentation.dto.response.NewsFindAllResponse;
import com.shoutoutz.api.news.presentation.dto.response.NoticeCreateResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    // TODO: 인증 객체 ROLE 조회 필요
    @PostMapping("/notices")
    public ResponseEntity<SuccessResponse<NoticeCreateResponse>> createNotice(
            @Valid @RequestBody NoticeCreateRequest body
    ) {
        CreateNoticeResult result = newsService.createNotice(body.toCommand());
        NoticeCreateResponse response = NoticeCreateResponse.from(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
    }

    // TODO: 인증 객체 ROLE 조회 필요
    @PostMapping("/events")
    public ResponseEntity<SuccessResponse<EventCreateResponse>> createEvent(
            @Valid @RequestBody EventCreateRequest body
    ) {
        CreateEventResult result = newsService.createEvent(body.toCommand());
        EventCreateResponse response = EventCreateResponse.from(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<List<NewsFindAllResponse.Item>>> findAll(
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(required = false) String eventStatus,
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(required = false) String cursor
    ) {
        NewsFindAllResult result = newsService.findAll(
                new NewsFindAllRequest(type, eventStatus, sort, size, cursor).toQuery());
        NewsFindAllResponse response = NewsFindAllResponse.from(result);
        return ResponseEntity.ok(SuccessResponse.success(response.items(), response.meta()));
    }
}
