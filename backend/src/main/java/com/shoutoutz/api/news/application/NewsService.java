package com.shoutoutz.api.news.application;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.news.application.command.CreateEventCommand;
import com.shoutoutz.api.news.application.command.CreateNoticeCommand;
import com.shoutoutz.api.news.application.dto.result.CreateEventResult;
import com.shoutoutz.api.news.application.dto.result.CreateNoticeResult;
import com.shoutoutz.api.news.application.dto.result.NewsFindAllResult;
import com.shoutoutz.api.news.application.query.NewsCursor;
import com.shoutoutz.api.news.application.query.NewsFindAllQuery;
import com.shoutoutz.api.news.application.query.NewsPage;
import com.shoutoutz.api.news.application.query.NewsQueryRepository;
import com.shoutoutz.api.news.application.query.NewsSummary;
import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsCta;
import com.shoutoutz.api.news.domain.NewsRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsQueryRepository newsQueryRepository;
    private final Clock clock;

    @Transactional
    public CreateNoticeResult createNotice(CreateNoticeCommand command) {
        NewsCta cta = toCta(command.cta());
        Instant now = clock.instant();
        News notice = News.createNotice(
                command.title(),
                command.summary(),
                command.body(),
                resolveAuthorId(),
                command.authorName(),
                cta,
                now
        );
        return CreateNoticeResult.from(newsRepository.save(notice));
    }

    @Transactional
    public CreateEventResult createEvent(CreateEventCommand command) {
        NewsCta cta = toCta(command.cta());
        Instant now = clock.instant();
        News event = News.createEvent(
                command.title(),
                command.summary(),
                command.body(),
                resolveAuthorId(),
                command.authorName(),
                command.eventStartAt(),
                command.eventEndAt(),
                cta,
                now
        );
        return CreateEventResult.from(newsRepository.save(event), now);
    }

    @Transactional(readOnly = true)
    public NewsFindAllResult findAll(NewsFindAllQuery query) {
        validateSize(query.size());

        NewsCursor cursor = NewsCursorCodec.decode(query.encodedCursor());
        Instant now = clock.instant();
        NewsPage page = newsQueryRepository.findAll(
                query.type(),
                query.eventStatus(),
                now,
                cursor,
                query.size()
        );

        List<NewsFindAllResult.Item> items = page.items().stream()
                .map(summary -> NewsFindAllResult.Item.from(summary, now))
                .toList();
        boolean hasNext = page.hasNext() && !items.isEmpty();
        String nextCursor = hasNext
                ? NewsCursorCodec.encode(lastCursor(page.items()))
                : null;
        return new NewsFindAllResult(
                items,
                new NewsFindAllResult.Meta(nextCursor, hasNext)
        );
    }

    private Long resolveAuthorId() {
        // TODO: 관리자 인증 권한 검증 후, 인증 주체의 authorId를 주입한다.
        return 0L;
    }

    private NewsCta toCta(CreateNoticeCommand.Cta cta) {
        return cta == null ? null : toCta(cta.label(), cta.url());
    }

    private NewsCta toCta(CreateEventCommand.Cta cta) {
        return cta == null ? null : toCta(cta.label(), cta.url());
    }

    private NewsCta toCta(String label, String url) {
        return new NewsCta(label, url);
    }

    private void validateSize(int size) {
        if (size < 1 || size > 50) {
            throw new BadRequestException(NewsQueryErrorCode.NEWS_INVALID_SIZE);
        }
    }

    private NewsCursor lastCursor(List<NewsSummary> items) {
        NewsSummary lastSummary = items.get(items.size() - 1);
        return new NewsCursor(lastSummary.publishedAt(), lastSummary.id());
    }
}
