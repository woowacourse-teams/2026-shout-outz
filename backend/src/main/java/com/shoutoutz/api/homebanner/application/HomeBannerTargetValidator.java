package com.shoutoutz.api.homebanner.application;

import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.feed.domain.FeedRepository;
import com.shoutoutz.api.homebanner.domain.BannerTargetType;
import com.shoutoutz.api.homebanner.domain.HomeBannerErrorCode;
import com.shoutoutz.api.news.application.NewsQueryRepository;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.Slug;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HomeBannerTargetValidator {

    private final NewsQueryRepository newsQueryRepository;
    private final ProjectRepository projectRepository;
    private final FeedRepository feedRepository;

    /**
     * 대상이 실제로 있는지 확인하고, 저장할 대상 ID를 돌려준다.
     * 프로젝트는 slug로 받아 ID로 바꾼다. 형식이 틀린 slug도 없는 대상과 같게 다룬다.
     */
    public Long resolveTargetId(BannerTargetType targetType, Long targetId, String targetSlug) {
        if (targetType == null) {
            return null;
        }

        return switch (targetType) {
            case NEWS -> requireExists(targetId, newsQueryRepository.findDetailById(targetId, false).isPresent());
            case PROJECT -> resolvePublicProjectId(targetSlug);
            case FEED -> requireExists(targetId, feedRepository.findActiveById(targetId).isPresent());
        };
    }

    private Long resolvePublicProjectId(String targetSlug) {
        return Slug.parse(targetSlug)
                .flatMap(projectRepository::findIdBySlug)
                .filter(projectRepository::existsPublicById)
                .orElseThrow(HomeBannerTargetValidator::targetNotFound);
    }

    private Long requireExists(Long targetId, boolean exists) {
        if (!exists) {
            throw targetNotFound();
        }
        return targetId;
    }

    private static NotFoundException targetNotFound() {
        return new NotFoundException(HomeBannerErrorCode.HOME_BANNER_TARGET_NOT_FOUND);
    }
}
