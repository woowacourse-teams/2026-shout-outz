package com.shoutoutz.api.homebanner.application;

import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.NotFoundException;
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
        targetValidator.validate(request.targetType(), request.targetId());

        HomeBanner saved = homeBannerRepository.save(request.toHomeBanner(userId));
        return HomeBannerAdminResponse.from(saved, imageUrl);
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
        targetValidator.validate(request.targetType(), request.targetId());

        HomeBanner updated = banner.update(
                request.mediaId(),
                request.destinationType(),
                request.targetType(),
                request.targetId(),
                request.linkType(),
                request.linkUrl(),
                request.displayOrder(),
                request.active()
        );
        HomeBanner saved = homeBannerRepository.update(updated)
                .orElseThrow(() -> new NotFoundException(HomeBannerErrorCode.HOME_BANNER_NOT_FOUND));
        return HomeBannerAdminResponse.from(saved, imageUrl);
    }

    @Transactional
    public void delete(long bannerId, UserRole role) {
        validateAdmin(role);
        if (!homeBannerRepository.deleteById(bannerId)) {
            throw new NotFoundException(HomeBannerErrorCode.HOME_BANNER_NOT_FOUND);
        }
    }

    private HomeBannerAdminResponse toResponse(HomeBanner banner) {
        return HomeBannerAdminResponse.from(
                banner,
                imageService.createImageUrl(banner.getMediaId())
        );
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
