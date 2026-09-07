package com.shoutoutz.api.news.application;

import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsCta;
import com.shoutoutz.api.news.domain.NewsRepository;
import com.shoutoutz.api.news.presentation.dto.request.NoticeCreateRequest;
import com.shoutoutz.api.news.presentation.dto.response.NoticeCreateResponse;
import java.time.Clock;
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
}
