package com.shoutoutz.api.magazine.application;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.magazine.application.dto.MagazineCursor;
import com.shoutoutz.api.magazine.application.dto.MagazineItem;
import com.shoutoutz.api.magazine.application.dto.MagazinePage;
import com.shoutoutz.api.magazine.domain.Magazine;
import com.shoutoutz.api.magazine.domain.MagazineErrorCode;
import com.shoutoutz.api.magazine.domain.MagazineReactionCounts;
import com.shoutoutz.api.magazine.domain.MagazineReactionRepository;
import com.shoutoutz.api.magazine.domain.MagazineRepository;
import com.shoutoutz.api.magazine.domain.MagazineStatus;
import com.shoutoutz.api.magazine.presentation.dto.request.MagazineSaveRequest;
import com.shoutoutz.api.magazine.presentation.dto.response.MagazineAdminResponse;
import com.shoutoutz.api.magazine.presentation.dto.response.MagazineDetailResponse;
import com.shoutoutz.api.magazine.presentation.dto.response.MagazineSummaryResponse;
import com.shoutoutz.api.magazine.presentation.dto.response.FeaturedUserResponse;
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
public class MagazineService {
    private static final ZoneId VIEW_DATE_ZONE = ZoneId.of("Asia/Seoul");
    private static final Pattern HTML_IMAGE = Pattern.compile("<img\\b", Pattern.CASE_INSENSITIVE);
    private static final MagazineReactionCounts NO_REACTION = new MagazineReactionCounts(0L, false);

    private final MagazineRepository magazineRepository;
    private final MagazineQueryRepository magazineQueryRepository;
    private final MagazineReactionRepository magazineReactionRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final MediaMetadataRepository mediaMetadataRepository;
    private final MediaUrlResolver mediaUrlResolver;
    private final UserAvatarUrlResolver userAvatarUrlResolver;
    private final Clock clock;

    @Transactional
    public MagazineAdminResponse create(long editorId, MagazineSaveRequest request) {
        validateAdmin(editorId);
        validateFeaturedUser(request.featuredUserId());
        validateMedia(null, request.thumbnailMediaId(), request.contentMd(), editorId);
        Magazine magazine = Magazine.builder()
                .title(request.title().strip())
                .summary(request.summary())
                .featuredUserId(request.featuredUserId())
                .thumbnailMediaId(request.thumbnailMediaId())
                .contentMd(request.contentMd())
                .authorId(editorId)
                .status(MagazineStatus.DRAFT)
                .build();
        return adminResponse(magazineRepository.save(magazine));
    }

    @Transactional
    public MagazineAdminResponse update(long id, long editorId, MagazineSaveRequest request) {
        validateAdmin(editorId);
        Magazine magazine = findForUpdate(id);
        validateFeaturedUser(request.featuredUserId());
        String contentMd = normalizeContent(magazine, request.contentMd());
        validateMedia(magazine, request.thumbnailMediaId(), contentMd, editorId);
        magazine.edit(request.title().strip(), request.summary(), request.featuredUserId(),
                request.thumbnailMediaId(), contentMd);
        return adminResponse(magazineRepository.save(magazine));
    }

    @Transactional
    public MagazineAdminResponse publish(long id, long editorId) {
        validateAdmin(editorId);
        Magazine magazine = findForUpdate(id);
        validateFeaturedUser(magazine.getFeaturedUserId());
        validateMedia(magazine, magazine.getThumbnailMediaId(), magazine.getContentMd(), editorId);
        magazine.publish(clock.instant());
        return adminResponse(magazineRepository.save(magazine));
    }

    @Transactional
    public MagazineAdminResponse unpublish(long id, long editorId) {
        validateAdmin(editorId);
        Magazine magazine = findForUpdate(id);
        magazine.unpublish();
        return adminResponse(magazineRepository.save(magazine));
    }

    @Transactional
    public void delete(long id, long editorId) {
        validateAdmin(editorId);
        Magazine magazine = findForUpdate(id);
        magazine.delete(clock.instant());
        magazineRepository.save(magazine);
    }

    public MagazinePage findAll(String cursor, int size) {
        return findAll(cursor, size, null);
    }

    public MagazinePage findAll(String cursor, int size, Long viewerId) {
        return page(cursor, size, false, viewerId);
    }

    public MagazinePage findAllAdmin(long editorId, String cursor, int size) {
        validateAdmin(editorId);
        return page(cursor, size, true, editorId);
    }

    public MagazineAdminResponse findAdmin(long id, long editorId) {
        validateAdmin(editorId);
        Magazine magazine = findActive(id);
        return adminResponse(magazine);
    }

    @Transactional
    public MagazineDetailResponse find(long id, Long viewerId, VisitorKey visitorKey) {
        MagazineItem item = magazineQueryRepository.findPublished(id)
                .orElseThrow(() -> new EntityNotFoundException(MagazineErrorCode.MAGAZINE_NOT_FOUND));
        Map<Long, URI> urls = contentUrls(item.contentMd());
        Map<Long, String> avatarUrls = resolveAvatarUrls(List.of(item));
        MagazineReactionCounts counts = magazineReactionRepository.findCountsByMagazineId(id, viewerId);
        URI thumbnailUrl = mediaUrlResolver.resolve(item.thumbnailMediaId(), MediaVariant.THUMBNAIL);
        String contentMd = mediaUrlResolver.replaceDescriptionReferences(item.contentMd(), urls);
        Instant viewedAt = clock.instant();
        LocalDate viewedOn = LocalDate.ofInstant(viewedAt, VIEW_DATE_ZONE);
        long viewCount = magazineRepository.recordView(id, visitorKey, viewedOn, viewedAt);
        return MagazineDetailResponse.from(
                item,
                thumbnailUrl,
                featuredUser(item, avatarUrls),
                contentMd,
                counts,
                viewCount
        );
    }

    private MagazinePage page(String cursor, int size, boolean admin, Long viewerId) {
        if (size < 1 || size > 50) {
            throw new InvalidInputException(MagazineErrorCode.MAGAZINE_INVALID_SIZE);
        }
        String scope = "PUBLIC";
        if (admin) {
            scope = "ADMIN";
        }
        MagazineCursor decoded = MagazineCursorCodec.decode(cursor, scope);
        List<MagazineItem> items = magazineQueryRepository.findPage(decoded, size + 1, admin);
        boolean hasNext = items.size() > size;
        if (hasNext) {
            items = items.subList(0, size);
        }
        Map<Long, String> avatarUrls = resolveAvatarUrls(items);
        Set<Long> thumbnailIds = new HashSet<>();
        for (MagazineItem item : items) {
            if (item.thumbnailMediaId() != null) {
                thumbnailIds.add(item.thumbnailMediaId());
            }
        }
        Map<Long, URI> thumbnailUrls = mediaUrlResolver.resolveAll(thumbnailIds, MediaVariant.THUMBNAIL);
        Map<Long, MagazineReactionCounts> reactions = magazineReactionRepository.findCountsByMagazineIds(
                items.stream().map(MagazineItem::id).toList(), viewerId);
        List<MagazineSummaryResponse> responses = new ArrayList<>();
        for (MagazineItem item : items) {
            MagazineReactionCounts counts = reactions.getOrDefault(item.id(), NO_REACTION);
            responses.add(MagazineSummaryResponse.from(item,
                    mediaUrl(thumbnailUrls, item.thumbnailMediaId()), featuredUser(item, avatarUrls), counts));
        }
        String nextCursor = null;
        if (hasNext) {
            MagazineItem last = items.getLast();
            Instant timestamp = last.publishedAt();
            if (admin) {
                timestamp = last.createdAt();
            }
            nextCursor = MagazineCursorCodec.encode(timestamp, last.id(), scope);
        }
        return new MagazinePage(responses, new SliceMetaResponse(nextCursor, hasNext, magazineQueryRepository.count(admin)));
    }

    private Map<Long, URI> contentUrls(String contentMd) {
        return mediaUrlResolver.resolveAll(DescriptionMediaReferences.extractMediaIds(contentMd));
    }

    private Map<Long, String> resolveAvatarUrls(List<MagazineItem> items) {
        List<UserAvatarUrlResolver.AvatarReference> references = items.stream()
                .map(item -> new UserAvatarUrlResolver.AvatarReference(item.featuredUserId(), item.avatarImageId()))
                .toList();
        return userAvatarUrlResolver.resolveAll(references);
    }

    private URI mediaUrl(Map<Long, URI> urls, Long id) {
        if (id == null) {
            return null;
        }
        return urls.get(id);
    }

    private FeaturedUserResponse featuredUser(MagazineItem item, Map<Long, String> avatarUrls) {
        String avatarUrl = null;
        if (item.featuredUserId() != null) {
            avatarUrl = avatarUrls.get(item.featuredUserId());
        }
        return new FeaturedUserResponse(item.handle(), item.displayName(), item.bio(), avatarUrl);
    }

    private void validateFeaturedUser(long userId) {
        userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException(MagazineErrorCode.FEATURED_USER_NOT_FOUND));
        userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(MagazineErrorCode.FEATURED_USER_NOT_FOUND));
    }

    private void validateAdmin(long userId) {
        userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE && user.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new ForbiddenException(MagazineErrorCode.MAGAZINE_ADMIN_FORBIDDEN));
    }

    private Magazine findActive(long id) {
        return magazineRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException(MagazineErrorCode.MAGAZINE_NOT_FOUND));
    }

    private Magazine findForUpdate(long id) {
        return magazineRepository.findForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException(MagazineErrorCode.MAGAZINE_NOT_FOUND));
    }

    private MagazineAdminResponse adminResponse(Magazine magazine) {
        Map<Long, URI> urls = contentUrls(magazine.getContentMd());
        return MagazineAdminResponse.from(magazine,
                mediaUrlResolver.resolve(magazine.getThumbnailMediaId(), MediaVariant.THUMBNAIL),
                mediaUrlResolver.replaceDescriptionReferences(magazine.getContentMd(), urls));
    }

    private String normalizeContent(Magazine existing, String contentMd) {
        Map<Long, URI> urls = mediaUrlResolver.resolveAll(DescriptionMediaReferences.extractMediaIds(existing.getContentMd()));
        return mediaUrlResolver.replaceDescriptionUrlsWithReferences(contentMd, urls);
    }

    private void validateMedia(Magazine existing, Long thumbnailId, String contentMd, long editorId) {
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
            validateMedia(thumbnailId, MediaPurpose.MAGAZINE_THUMBNAIL, editorId,
                    thumbnailId.equals(existingThumbnail));
        }
        for (Long id : DescriptionMediaReferences.extractMediaIds(contentMd)) {
            validateMedia(id, MediaPurpose.MAGAZINE_CONTENT, editorId, existingContent.contains(id));
        }
    }

    private void validateMedia(long id, MediaPurpose purpose, long editorId, boolean alreadyAttached) {
        MediaMetadata media = mediaMetadataRepository.findById(id).orElseThrow(this::invalidMedia);
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
        return new InvalidInputException(MagazineErrorCode.MAGAZINE_INVALID_MEDIA);
    }
}
