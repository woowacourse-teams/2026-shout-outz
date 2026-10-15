package com.shoutoutz.api.interview.application;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.interview.application.dto.InterviewCursor;
import com.shoutoutz.api.interview.application.dto.InterviewItem;
import com.shoutoutz.api.interview.application.dto.InterviewPage;
import com.shoutoutz.api.interview.domain.InterviewErrorCode;
import com.shoutoutz.api.interview.domain.InterviewReactionRepository;
import com.shoutoutz.api.interview.domain.InterviewReactionCounts;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewAdminResponse;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewDetailResponse;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewSummaryResponse;
import com.shoutoutz.api.interview.presentation.dto.response.IntervieweeResponse;
import com.shoutoutz.api.media.application.MediaUrlResolver;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import com.shoutoutz.api.project.domain.DescriptionMediaReferences;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewQueryService {
    private static final InterviewReactionCounts NO_REACTION = new InterviewReactionCounts(0L, false);
    private final InterviewReactionRepository reactionRepository;
    private final InterviewQueryRepository repository;
    private final InterviewAccessService accessService;
    private final MediaUrlResolver mediaUrlResolver;

    public InterviewPage findAll(String cursor, int size) {
        return findAll(cursor, size, null);
    }

    public InterviewPage findAll(String cursor, int size, Long viewerId) {
        return page(cursor, size, false, viewerId);
    }

    public InterviewPage findAllAdmin(long editorId, String cursor, int size) {
        accessService.requireAdmin(editorId);
        return page(cursor, size, true, editorId);
    }

    public InterviewAdminResponse findAdmin(long id, long editorId) {
        accessService.requireAdmin(editorId);
        return InterviewAdminResponse.from(accessService.requireActive(id));
    }

    public InterviewDetailResponse find(long id) {
        return find(id, null);
    }

    public InterviewDetailResponse find(long id, Long viewerId) {
        InterviewItem item = repository.findPublished(id)
                .orElseThrow(() -> new EntityNotFoundException(InterviewErrorCode.INTERVIEW_NOT_FOUND));
        Map<Long, URI> urls = resolveDisplayUrls(List.of(item), true);
        InterviewReactionCounts counts = reactionRepository.findCountsByInterviewId(id, viewerId);
        return new InterviewDetailResponse(item.id(), item.title(), item.summary(),
                mediaUrlResolver.resolve(item.thumbnailMediaId(), MediaVariant.THUMBNAIL), interviewee(item, urls),
                mediaUrlResolver.replaceDescriptionReferences(item.contentMd(), urls),
                item.publishedAt(), item.createdAt(), item.updatedAt(), counts.likeCount(), counts.likedByMe(), item.commentCount());
    }

    private InterviewPage page(String cursor, int size, boolean admin, Long viewerId) {
        if (size < 1 || size > 50) {
            throw new InvalidInputException(InterviewErrorCode.INTERVIEW_INVALID_SIZE);
        }
        String scope = "PUBLIC";
        if (admin) {
            scope = "ADMIN";
        }
        InterviewCursor decoded = InterviewCursorCodec.decode(cursor, scope);
        List<InterviewItem> items = repository.findPage(decoded, size + 1, admin);
        boolean hasNext = items.size() > size;
        if (hasNext) {
            items = items.subList(0, size);
        }
        Map<Long, URI> displayUrls = resolveDisplayUrls(items, false);
        Set<Long> thumbnailIds = new HashSet<>();
        for (InterviewItem item : items) {
            if (item.thumbnailMediaId() != null) {
                thumbnailIds.add(item.thumbnailMediaId());
            }
        }
        Map<Long, URI> thumbnailUrls = mediaUrlResolver.resolveAll(thumbnailIds, MediaVariant.THUMBNAIL);
        Map<Long, InterviewReactionCounts> reactions = reactionRepository.findCountsByInterviewIds(
                items.stream().map(InterviewItem::id).toList(), viewerId);
        List<InterviewSummaryResponse> responses = new ArrayList<>();
        for (InterviewItem item : items) {
            InterviewReactionCounts counts = reactions.getOrDefault(item.id(), NO_REACTION);
            responses.add(new InterviewSummaryResponse(item.id(), item.title(), item.summary(),
                    mediaUrl(thumbnailUrls, item.thumbnailMediaId()), interviewee(item, displayUrls),
                    item.status(), item.publishedAt(), item.createdAt(), item.updatedAt(),
                    counts.likeCount(), counts.likedByMe(), item.commentCount()));
        }
        String nextCursor = null;
        if (hasNext) {
            InterviewItem last = items.getLast();
            Instant timestamp = last.publishedAt();
            if (admin) {
                timestamp = last.createdAt();
            }
            nextCursor = InterviewCursorCodec.encode(timestamp, last.id(), scope);
        }
        return new InterviewPage(responses, new SliceMetaResponse(nextCursor, hasNext, repository.count(admin)));
    }

    private Map<Long, URI> resolveDisplayUrls(List<InterviewItem> items, boolean includeContent) {
        Set<Long> ids = new HashSet<>();
        for (InterviewItem item : items) {
            if (item.avatarImageId() != null) {
                ids.add(item.avatarImageId());
            }
            if (includeContent) {
                ids.addAll(DescriptionMediaReferences.extractMediaIds(item.contentMd()));
            }
        }
        return mediaUrlResolver.resolveAll(ids);
    }

    private URI mediaUrl(Map<Long, URI> urls, Long id) {
        if (id == null) {
            return null;
        }
        return urls.get(id);
    }

    private IntervieweeResponse interviewee(InterviewItem item, Map<Long, URI> urls) {
        return new IntervieweeResponse(item.handle(), item.displayName(), item.bio(), mediaUrl(urls, item.avatarImageId()));
    }
}
