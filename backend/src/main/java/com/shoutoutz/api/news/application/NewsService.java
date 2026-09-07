package com.shoutoutz.api.news.application;

import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsCta;
import com.shoutoutz.api.news.domain.NewsRepository;
import com.shoutoutz.api.news.presentation.dto.request.EventCreateRequest;
import com.shoutoutz.api.news.presentation.dto.request.NoticeCreateRequest;
import com.shoutoutz.api.news.presentation.dto.response.EventCreateResponse;
import com.shoutoutz.api.news.presentation.dto.response.NoticeCreateResponse;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final Clock clock;

    @Transactional
    public NoticeCreateResponse createNotice(NoticeCreateRequest body) {
        //TODO: 관리자 인증 권한 검증 후, authorId 꺼내기
        Long authorId = 0L;
        NewsCta cta = body.cta() == null ? null : new NewsCta(body.cta());
        News notice = News.createNotice(body, authorId, cta, clock.instant());

        News savedNotice = newsRepository.save(notice);

        return NoticeCreateResponse.from(savedNotice);
    }

    @Transactional
    public EventCreateResponse createEvent(EventCreateRequest body) {
        //TODO: 관리자 인증 권한 검증 후, authorId 꺼내기
        Long authorId = 0L;
        NewsCta cta = body.cta() == null
                ? null
                : new NewsCta(body.cta().label(), body.cta().url());
        Instant now = clock.instant();
        News event = News.createEvent(body, authorId, cta, now);

        News savedEvent = newsRepository.save(event);

        return EventCreateResponse.from(savedEvent, now);
    }
}
