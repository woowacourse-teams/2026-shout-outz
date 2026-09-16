package com.shoutoutz.api.homebanner.application;

import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.feed.domain.FeedRepository;
import com.shoutoutz.api.homebanner.domain.BannerTargetType;
import com.shoutoutz.api.homebanner.domain.HomeBannerErrorCode;
import com.shoutoutz.api.news.application.NewsQueryRepository;
import com.shoutoutz.api.project.domain.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HomeBannerTargetValidator {

    private final NewsQueryRepository newsQueryRepository;
    private final ProjectRepository projectRepository;
    private final FeedRepository feedRepository;

    public void validate(BannerTargetType targetType, Long targetId) {
        if (targetType == null) {
            return;
        }

        boolean exists = switch (targetType) {
            case NEWS -> newsQueryRepository.findDetailById(targetId, false).isPresent();
            case PROJECT -> projectRepository.existsPublicById(targetId);
            case FEED -> feedRepository.findActiveById(targetId).isPresent();
        };
        if (!exists) {
            throw new NotFoundException(HomeBannerErrorCode.HOME_BANNER_TARGET_NOT_FOUND);
        }
    }
}
