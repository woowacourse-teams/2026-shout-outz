package com.shoutoutz.api.interview.application;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.interview.application.dto.InterviewCursor;
import com.shoutoutz.api.interview.application.dto.InterviewItem;
import com.shoutoutz.api.interview.application.dto.InterviewPage;
import com.shoutoutz.api.interview.domain.Interview;
import com.shoutoutz.api.interview.domain.InterviewErrorCode;
import com.shoutoutz.api.interview.domain.InterviewReactionCounts;
import com.shoutoutz.api.interview.domain.InterviewReactionRepository;
import com.shoutoutz.api.interview.domain.InterviewRepository;
import com.shoutoutz.api.interview.domain.InterviewStatus;
import com.shoutoutz.api.interview.presentation.dto.request.InterviewSaveRequest;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewAdminResponse;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewDetailResponse;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewSummaryResponse;
import com.shoutoutz.api.interview.presentation.dto.response.IntervieweeResponse;
import com.shoutoutz.api.media.application.MediaUrlResolver;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import com.shoutoutz.api.project.domain.DescriptionMediaReferences;
import com.shoutoutz.api.user.application.UserAvatarUrlResolver;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {
    private static final ZoneId VIEW_DATE_ZONE = ZoneId.of("Asia/Seoul");
    private static final Pattern HTML_IMAGE = Pattern.compile("<img\\b", Pattern.CASE_INSENSITIVE);
    private static final InterviewReactionCounts NO_REACTION = new InterviewReactionCounts(0L, false);

    private final InterviewRepository repository;
    private final InterviewQueryRepository queryRepository;
    private final InterviewReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final MediaMetadataRepository mediaRepository;
    private final MediaUrlResolver mediaUrlResolver;
    private final UserAvatarUrlResolver avatarUrlResolver;
    private final Clock clock;

    @Transactional
    public InterviewAdminResponse create(long editorId, InterviewSaveRequest request) {
        validateAdmin(editorId);
        validateInterviewee(request.intervieweeId());
        validateMedia(null, request.thumbnailMediaId(), request.contentMd(), editorId);
        Interview interview = Interview.builder()
                .title(request.title().strip())
                .summary(request.summary())
                .intervieweeId(request.intervieweeId())
                .thumbnailMediaId(request.thumbnailMediaId())
                .contentMd(request.contentMd())
                .authorId(editorId)
                .status(InterviewStatus.DRAFT)
                .build();
        return adminResponse(repository.save(interview));
    }

    @Transactional
    public InterviewAdminResponse update(long id, long editorId, InterviewSaveRequest request) {
        validateAdmin(editorId);
        Interview interview = findForUpdate(id);
        validateInterviewee(request.intervieweeId());
        String contentMd = normalizeContent(interview, request.contentMd());
        validateMedia(interview, request.thumbnailMediaId(), contentMd, editorId);
        interview.edit(request.title().strip(), request.summary(), request.intervieweeId(),
                request.thumbnailMediaId(), contentMd);
        return adminResponse(repository.save(interview));
    }

    @Transactional
    public InterviewAdminResponse publish(long id, long editorId) {
        validateAdmin(editorId);
        Interview interview = findForUpdate(id);
        validateInterviewee(interview.getIntervieweeId());
        validateMedia(interview, interview.getThumbnailMediaId(), interview.getContentMd(), editorId);
        interview.publish(clock.instant());
        return adminResponse(repository.save(interview));
    }

    @Transactional
    public InterviewAdminResponse unpublish(long id, long editorId) {
        validateAdmin(editorId);
        Interview interview = findForUpdate(id);
        interview.unpublish();
        return adminResponse(repository.save(interview));
    }

    @Transactional
    public void delete(long id, long editorId) {
        validateAdmin(editorId);
        Interview interview = findForUpdate(id);
        interview.delete(clock.instant());
        repository.save(interview);
    }

    public InterviewPage findAll(String cursor, int size) {
        return findAll(cursor, size, null);
    }

    public InterviewPage findAll(String cursor, int size, Long viewerId) {
        return page(cursor, size, false, viewerId);
    }

    public InterviewPage findAllAdmin(long editorId, String cursor, int size) {
        validateAdmin(editorId);
        return page(cursor, size, true, editorId);
    }

    public InterviewAdminResponse findAdmin(long id, long editorId) {
        validateAdmin(editorId);
        Interview interview = findActive(id);
        return adminResponse(interview);
    }

    @Transactional
    public InterviewDetailResponse find(long id, Long viewerId, VisitorKey visitorKey) {
        findForUpdate(id).validatePublished();
        InterviewItem item = queryRepository.findPublished(id)
                .orElseThrow(() -> new EntityNotFoundException(InterviewErrorCode.INTERVIEW_NOT_FOUND));
        Instant viewedAt = clock.instant();
        LocalDate viewedOn = LocalDate.ofInstant(viewedAt, VIEW_DATE_ZONE);
        long viewCount = repository.recordView(id, visitorKey, viewedOn, viewedAt);
        Map<Long, URI> urls = contentUrls(item.contentMd());
        Map<Long, String> avatarUrls = resolveAvatarUrls(List.of(item));
        InterviewReactionCounts counts = reactionRepository.findCountsByInterviewId(id, viewerId);
        return InterviewDetailResponse.from(item,
                mediaUrlResolver.resolve(item.thumbnailMediaId(), MediaVariant.THUMBNAIL),
                interviewee(item, avatarUrls),
                mediaUrlResolver.replaceDescriptionReferences(item.contentMd(), urls),
                counts, viewCount);
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
        List<InterviewItem> items = queryRepository.findPage(decoded, size + 1, admin);
        boolean hasNext = items.size() > size;
        if (hasNext) {
            items = items.subList(0, size);
        }
        Map<Long, String> avatarUrls = resolveAvatarUrls(items);
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
            responses.add(InterviewSummaryResponse.from(item,
                    mediaUrl(thumbnailUrls, item.thumbnailMediaId()), interviewee(item, avatarUrls), counts));
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
        return new InterviewPage(responses, new SliceMetaResponse(nextCursor, hasNext, queryRepository.count(admin)));
    }

    private Map<Long, URI> contentUrls(String contentMd) {
        return mediaUrlResolver.resolveAll(DescriptionMediaReferences.extractMediaIds(contentMd));
    }

    private Map<Long, String> resolveAvatarUrls(List<InterviewItem> items) {
        List<UserAvatarUrlResolver.AvatarReference> references = items.stream()
                .map(item -> new UserAvatarUrlResolver.AvatarReference(item.intervieweeUserId(), item.avatarImageId()))
                .toList();
        return avatarUrlResolver.resolveAll(references);
    }

    private URI mediaUrl(Map<Long, URI> urls, Long id) {
        if (id == null) {
            return null;
        }
        return urls.get(id);
    }

    private IntervieweeResponse interviewee(InterviewItem item, Map<Long, String> avatarUrls) {
        String avatarUrl = null;
        if (item.intervieweeUserId() != null) {
            avatarUrl = avatarUrls.get(item.intervieweeUserId());
        }
        return new IntervieweeResponse(item.handle(), item.displayName(), item.bio(), avatarUrl);
    }

    private void validateInterviewee(long userId) {
        userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException(InterviewErrorCode.INTERVIEWEE_NOT_FOUND));
        profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(InterviewErrorCode.INTERVIEWEE_NOT_FOUND));
    }

    private void validateAdmin(long userId) {
        userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE && user.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new ForbiddenException(InterviewErrorCode.INTERVIEW_ADMIN_FORBIDDEN));
    }

    private Interview findActive(long id) {
        return repository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException(InterviewErrorCode.INTERVIEW_NOT_FOUND));
    }

    private Interview findForUpdate(long id) {
        return repository.findForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException(InterviewErrorCode.INTERVIEW_NOT_FOUND));
    }

    private InterviewAdminResponse adminResponse(Interview interview) {
        Map<Long, URI> urls = contentUrls(interview.getContentMd());
        return InterviewAdminResponse.from(interview,
                mediaUrlResolver.resolve(interview.getThumbnailMediaId(), MediaVariant.THUMBNAIL),
                mediaUrlResolver.replaceDescriptionReferences(interview.getContentMd(), urls));
    }

    private String normalizeContent(Interview existing, String contentMd) {
        Map<Long, URI> urls = mediaUrlResolver.resolveAll(DescriptionMediaReferences.extractMediaIds(existing.getContentMd()));
        return mediaUrlResolver.replaceDescriptionUrlsWithReferences(contentMd, urls);
    }

    private void validateMedia(Interview existing, Long thumbnailId, String contentMd, long editorId) {
        if (mediaUrlResolver.containsUnsupportedDescriptionImageReference(contentMd)
                || containsHtmlImage(contentMd)) {
            throw invalidMedia();
        }
        Set<Long> existingContent = new HashSet<>();
        Long existingThumbnail = null;
        if (existing != null) {
            existingContent.addAll(DescriptionMediaReferences.extractMediaIds(existing.getContentMd()));
            existingThumbnail = existing.getThumbnailMediaId();
        }
        if (thumbnailId != null) {
            validateMedia(thumbnailId, MediaPurpose.INTERVIEW_THUMBNAIL, editorId,
                    thumbnailId.equals(existingThumbnail));
        }
        for (Long id : DescriptionMediaReferences.extractMediaIds(contentMd)) {
            validateMedia(id, MediaPurpose.INTERVIEW_CONTENT, editorId, existingContent.contains(id));
        }
    }

    private void validateMedia(long id, MediaPurpose purpose, long editorId, boolean alreadyAttached) {
        MediaMetadata media = mediaRepository.findById(id).orElseThrow(this::invalidMedia);
        if (media.getPurpose() != purpose || media.getStatus() != MediaStatus.READY) {
            throw invalidMedia();
        }
        if (!alreadyAttached && !Long.valueOf(editorId).equals(media.getUploadedBy())) {
            throw invalidMedia();
        }
    }

    private boolean containsHtmlImage(String content) {
        if (content == null) {
            return false;
        }
        return HTML_IMAGE.matcher(content).find();
    }

    private InvalidInputException invalidMedia() {
        return new InvalidInputException(InterviewErrorCode.INTERVIEW_INVALID_MEDIA);
    }
}
