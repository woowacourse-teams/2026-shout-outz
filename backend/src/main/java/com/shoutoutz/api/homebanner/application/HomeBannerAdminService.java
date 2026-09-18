package com.shoutoutz.api.homebanner.application;

import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.homebanner.domain.BannerDestinationType;
import com.shoutoutz.api.homebanner.domain.BannerLinkType;
import com.shoutoutz.api.homebanner.domain.BannerTargetType;
import com.shoutoutz.api.homebanner.domain.HomeBanner;
import com.shoutoutz.api.homebanner.domain.HomeBannerErrorCode;
import com.shoutoutz.api.homebanner.domain.HomeBannerRepository;
import com.shoutoutz.api.homebanner.presentation.dto.request.HomeBannerUpsertRequest;
import com.shoutoutz.api.homebanner.presentation.dto.response.HomeBannerAdminResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeBannerAdminService {

    private final HomeBannerRepository homeBannerRepository;
    private final HomeBannerImageService imageService;
    private final HomeBannerTargetValidator targetValidator;

    @Transactional(readOnly = true)
    public List<HomeBannerAdminResponse> findAll(UserRole role) {
        validateAdmin(role);
        return homeBannerRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HomeBannerAdminResponse save(
            long userId,
            UserRole role,
            HomeBannerUpsertRequest request
    ) {
        validateAdmin(role);
        URI imageUrl = imageService.createImageUrl(request.mediaId());
        BannerTargetType targetType = targetTypeOf(request.targetType());
        targetValidator.validate(targetType, request.targetId());

        HomeBanner saved = homeBannerRepository.save(HomeBanner.create(
                request.mediaId(),
                BannerDestinationType.valueOf(request.destinationType()),
                targetType,
                request.targetId(),
                linkTypeOf(request.linkType()),
                request.linkUrl(),
                request.displayOrder(),
                request.active(),
                userId
        ));
        return toResponse(saved, imageUrl);
    }

    @Transactional
    public HomeBannerAdminResponse update(
            long bannerId,
            UserRole role,
            HomeBannerUpsertRequest request
    ) {
        validateAdmin(role);
        HomeBanner banner = findBanner(bannerId);
        URI imageUrl = imageService.createImageUrl(request.mediaId());
        BannerTargetType targetType = targetTypeOf(request.targetType());
        targetValidator.validate(targetType, request.targetId());

        HomeBanner updated = banner.update(
                request.mediaId(),
                BannerDestinationType.valueOf(request.destinationType()),
                targetType,
                request.targetId(),
                linkTypeOf(request.linkType()),
                request.linkUrl(),
                request.displayOrder(),
                request.active()
        );
        HomeBanner saved = homeBannerRepository.update(updated)
                .orElseThrow(() -> new NotFoundException(HomeBannerErrorCode.HOME_BANNER_NOT_FOUND));
        return toResponse(saved, imageUrl);
    }

    @Transactional
    public long delete(long bannerId, UserRole role) {
        validateAdmin(role);
        if (!homeBannerRepository.deleteById(bannerId)) {
            throw new NotFoundException(HomeBannerErrorCode.HOME_BANNER_NOT_FOUND);
        }
        return bannerId;
    }

    private HomeBannerAdminResponse toResponse(HomeBanner banner) {
        return toResponse(banner, imageService.createImageUrl(banner.getMediaId()));
    }

    private HomeBannerAdminResponse toResponse(HomeBanner banner, URI imageUrl) {
        return new HomeBannerAdminResponse(
                banner.getId(),
                banner.getMediaId(),
                imageUrl,
                banner.getDestinationType().name(),
                enumName(banner.getTargetType()),
                banner.getTargetId(),
                enumName(banner.getLinkType()),
                banner.getLinkUrl(),
                banner.getDisplayOrder(),
                banner.isActive(),
                banner.getCreatedBy(),
                banner.getCreatedAt(),
                banner.getUpdatedAt()
        );
    }

    private BannerTargetType targetTypeOf(String value) {
        return value == null ? null : BannerTargetType.valueOf(value);
    }

    private BannerLinkType linkTypeOf(String value) {
        return value == null ? null : BannerLinkType.valueOf(value);
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private HomeBanner findBanner(long bannerId) {
        return homeBannerRepository.findById(bannerId)
                .orElseThrow(() -> new NotFoundException(HomeBannerErrorCode.HOME_BANNER_NOT_FOUND));
    }

    private void validateAdmin(UserRole role) {
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException(HomeBannerErrorCode.HOME_BANNER_ADMIN_FORBIDDEN);
        }
    }
}
