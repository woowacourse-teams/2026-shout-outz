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
        NewsCta cta = toCta(body.cta());
        Instant now = clock.instant();
        News notice = News.createNotice(
                body.title(),
                body.summary(),
                body.body(),
                resolveAuthorId(),
                body.authorName(),
                cta,
                now
        );
        return NoticeCreateResponse.from(newsRepository.save(notice));
    }

    @Transactional
    public EventCreateResponse createEvent(EventCreateRequest body) {
        NewsCta cta = toCta(body.cta());
        Instant now = clock.instant();
        News event = News.createEvent(
                body.title(),
                body.summary(),
                body.body(),
                resolveAuthorId(),
                body.authorName(),
                body.eventStartAt(),
                body.eventEndAt(),
                cta,
                now
        );
        return EventCreateResponse.from(newsRepository.save(event), now);
    }

    private Long resolveAuthorId() {
        // TODO: 관리자 인증 권한 검증 후, 인증 주체의 authorId를 주입한다.
        return 0L;
    }

    private NewsCta toCta(NoticeCreateRequest.Cta cta) {
        return cta == null ? null : toCta(cta.label(), cta.url());
    }

    private NewsCta toCta(EventCreateRequest.Cta cta) {
        return cta == null ? null : toCta(cta.label(), cta.url());
    }

    private NewsCta toCta(String label, String url) {
        return new NewsCta(label, url);
    }
}
