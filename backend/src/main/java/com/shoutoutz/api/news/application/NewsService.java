package com.shoutoutz.api.news.application;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.common.exception.custom.ValidationFailedException;
import com.shoutoutz.api.common.response.ErrorResponse;
import com.shoutoutz.api.news.application.dto.NewsCursor;
import com.shoutoutz.api.news.application.dto.NewsDetail;
import com.shoutoutz.api.news.application.dto.NewsPage;
import com.shoutoutz.api.news.application.dto.NewsSummary;
import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsCta;
import com.shoutoutz.api.news.domain.NewsErrorCode;
import com.shoutoutz.api.news.domain.NewsEventPeriod;
import com.shoutoutz.api.news.domain.NewsRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.news.domain.enums.EventStatus;
import com.shoutoutz.api.news.domain.enums.NewsType;
import com.shoutoutz.api.news.presentation.dto.request.EventCreateRequest;
import com.shoutoutz.api.news.presentation.dto.request.NewsFindAllRequest;
import com.shoutoutz.api.news.presentation.dto.request.NewsFindRequest;
import com.shoutoutz.api.news.presentation.dto.request.NewsUpdateRequest;
import com.shoutoutz.api.news.presentation.dto.request.NoticeCreateRequest;
import com.shoutoutz.api.news.presentation.dto.response.EventCreateResponse;
import com.shoutoutz.api.news.presentation.dto.response.NewsFindAllResponse;
import com.shoutoutz.api.news.presentation.dto.response.NewsFindResponse;
import com.shoutoutz.api.news.presentation.dto.response.NewsDeleteResponse;
import com.shoutoutz.api.news.presentation.dto.response.NewsUpdateResponse;
import com.shoutoutz.api.news.presentation.dto.response.NoticeCreateResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
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
    public NoticeCreateResponse createNotice(
            long authorId,
            UserRole role,
            NoticeCreateRequest request
    ) {
        validateAdmin(role);
        NewsCta cta = toCta(request.cta());
        Instant now = clock.instant();
        News notice = News.createNotice(
                request.title(),
                request.summary(),
                request.body(),
                authorId,
                request.authorName(),
                cta,
                now
        );
        News savedNotice = newsRepository.save(notice);
        return new NoticeCreateResponse(
                savedNotice.getId(),
                savedNotice.getType(),
                savedNotice.getTitle(),
                savedNotice.getSummary(),
                savedNotice.getBody(),
                new NoticeCreateResponse.Author(savedNotice.getAuthorId(), savedNotice.getAuthorName()),
                savedNotice.getPublishedAt(),
                savedNotice.isPinned(),
                savedNotice.getPinOrder(),
                toNoticeCta(savedNotice.getCta())
        );
    }

    @Transactional
    public EventCreateResponse createEvent(
            long authorId,
            UserRole role,
            EventCreateRequest request
    ) {
        validateAdmin(role);
        NewsEventPeriod eventPeriod = resolveEventPeriod(
                NewsType.EVENT, request.eventStartAt(), request.eventEndAt());
        NewsCta cta = toCta(request.cta());
        Instant now = clock.instant();
        News event = News.createEvent(
                request.title(),
                request.summary(),
                request.body(),
                authorId,
                request.authorName(),
                eventPeriod.startAt(),
                eventPeriod.endAt(),
                cta,
                now
        );
        News savedEvent = newsRepository.save(event);
        return new EventCreateResponse(
                savedEvent.getId(),
                savedEvent.getType(),
                savedEvent.getTitle(),
                savedEvent.getSummary(),
                savedEvent.getBody(),
                new EventCreateResponse.Author(savedEvent.getAuthorId(), savedEvent.getAuthorName()),
                savedEvent.getPublishedAt(),
                savedEvent.eventStatusAt(now),
                savedEvent.getEventStartAt(),
                savedEvent.getEventEndAt(),
                savedEvent.isPinned(),
                savedEvent.getPinOrder(),
                toEventCta(savedEvent.getCta())
        );
    }

    @Transactional
    public NewsUpdateResponse update(long newsId, UserRole role, NewsUpdateRequest request) {
        validateAdmin(role);
        validateNewsId(newsId);
        News current = newsRepository.findActiveById(newsId)
                .orElseThrow(() -> new EntityNotFoundException(NewsErrorCode.NEWS_NOT_FOUND));

        NewsEventPeriod eventPeriod = resolveEventPeriod(
                current.getType(), request.eventStartAt(), request.eventEndAt());
        News updated = current.update(
                request.title(),
                request.summary(),
                request.body(),
                request.authorName(),
                eventPeriod,
                toCta(request.cta())
        );
        News saved = newsRepository.update(updated);
        return NewsUpdateResponse.from(saved, clock.instant());
    }

    @Transactional
    public NewsDeleteResponse delete(long newsId, UserRole role) {
        validateAdmin(role);
        validateNewsId(newsId);
        Instant deletedAt = clock.instant();
        if (!newsRepository.softDelete(newsId, deletedAt)) {
            throw new EntityNotFoundException(NewsErrorCode.NEWS_NOT_FOUND);
        }
        return new NewsDeleteResponse(newsId, deletedAt);
    }

    /**
     * 전체 목록 조회
     */
    @Transactional(readOnly = true)
    public NewsFindAllResponse findAll(NewsFindAllRequest request) {
        Instant now = clock.instant();
        NewsPage page = newsQueryRepository.findAll(
                request.getNewsType(),
                request.getEventStatus(),
                now,
                request.getCursor(),
                request.getSize()
        );

        // data 응답부
        List<NewsFindAllResponse.Item> items =
                page.items().stream()
                        .map(summary -> toResponses(summary, now))
                        .toList();

        // meta 응답부
        boolean hasNext = page.hasNext() && !items.isEmpty();
        String nextCursor = hasNext ? NewsCursorCodec.encode(lastCursor(page.items())) : null;

        //반환
        return new NewsFindAllResponse(items, new NewsFindAllResponse.Meta(nextCursor, hasNext));
    }

    @Transactional(readOnly = true)
    public NewsFindResponse findDetail(NewsFindRequest request) {
        // 조회
        NewsDetail newsDetail = newsQueryRepository.findDetailById(request.newsId(), request.navigation())
                .orElseThrow(() -> new EntityNotFoundException(NewsErrorCode.NEWS_NOT_FOUND));

        // 이벤트인 경우, 상태 생성
        EventStatus eventStatus = newsDetail.type() == NewsType.EVENT ? getEventStatus(newsDetail) : null;

        // 응답
        return new NewsFindResponse(
                newsDetail.id(),
                newsDetail.type(),
                newsDetail.title(),
                newsDetail.body(),
                new NewsFindResponse.Author(newsDetail.authorId(), newsDetail.authorName()),
                newsDetail.publishedAt(),
                eventStatus,
                newsDetail.eventStartAt(),
                newsDetail.eventEndAt(),
                newsDetail.pinned(),
                newsDetail.pinOrder(),
                toDetailCta(newsDetail.cta()),
                toNavigation(newsDetail.previous()),
                toNavigation(newsDetail.next())
        );
    }

    private void validateAdmin(UserRole role) {
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException(NewsErrorCode.NEWS_ADMIN_FORBIDDEN);
        }
    }

    private void validateNewsId(long newsId) {
        if (newsId <= 0) {
            throw new InvalidInputException(NewsErrorCode.NEWS_INVALID_ID_SIZE);
        }
    }

    private void validateEventPeriod(Instant startAt, Instant endAt) {
        if (startAt != null && endAt != null && startAt.isAfter(endAt)) {
            throw new BadRequestException(NewsErrorCode.NEWS_EVENT_PERIOD_INVALID);
        }
    }

    private NewsEventPeriod resolveEventPeriod(
            NewsType type,
            Instant startAt,
            Instant endAt
    ) {
        if (type == NewsType.NOTICE) {
            if (startAt != null || endAt != null) {
                throw new BadRequestException(NewsErrorCode.NEWS_EVENT_PERIOD_NOT_ALLOWED);
            }
            return null;
        }

        List<ErrorResponse.ErrorDetail> details = new ArrayList<>();
        if (startAt == null) {
            details.add(new ErrorResponse.ErrorDetail("eventStartAt", "eventStartAt은 필수입니다."));
        }
        if (endAt == null) {
            details.add(new ErrorResponse.ErrorDetail("eventEndAt", "eventEndAt은 필수입니다."));
        }
        if (!details.isEmpty()) {
            throw new ValidationFailedException(details);
        }
        validateEventPeriod(startAt, endAt);
        return new NewsEventPeriod(startAt, endAt);
    }

    private NewsCta toCta(NewsUpdateRequest.Cta cta) {
        return cta == null ? null : toCta(cta.label(), cta.url());
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

    private NoticeCreateResponse.Cta toNoticeCta(NewsCta cta) {
        return cta == null ? null : new NoticeCreateResponse.Cta(cta.label(), cta.url());
    }

    private EventCreateResponse.Cta toEventCta(NewsCta cta) {
        return cta == null ? null : new EventCreateResponse.Cta(cta.label(), cta.url());
    }

    /**
     * 전체 목록 조회 유스케이스 헬퍼메서드
     */
    private NewsFindAllResponse.Item toResponses
            (NewsSummary summary, Instant now) {
        EventStatus eventStatus = summary.type() == NewsType.EVENT
                ? EventStatus.from(now, summary.eventStartAt(), summary.eventEndAt())
                : null;
        return new NewsFindAllResponse.Item(
                summary.id(),
                summary.type(),
                summary.title(),
                summary.summary(),
                summary.publishedAt(),
                eventStatus,
                summary.eventStartAt(),
                summary.eventEndAt(),
                summary.pinned(),
                summary.pinOrder()
        );
    }

    private NewsCursor lastCursor(List<NewsSummary> items) {
        NewsSummary lastSummary = items.get(items.size() - 1);
        return new NewsCursor(lastSummary.publishedAt(), lastSummary.id());
    }

    /**
     * 상세 조회 유스케이스 헬퍼메서드
     */
    private NewsFindResponse.Cta toDetailCta(NewsDetail.Cta cta) {
        return cta == null ? null : new NewsFindResponse.Cta(cta.label(), cta.url());
    }

    private NewsFindResponse.Navigation toNavigation(NewsDetail.Navigation item) {
        return item == null ? null : new NewsFindResponse.Navigation(item.id(), item.title(), item.publishedAt());
    }

    private EventStatus getEventStatus(NewsDetail newsDetail) {
        Instant now = clock.instant();
        return EventStatus.from(now, newsDetail.eventStartAt(), newsDetail.eventEndAt());
    }
}
