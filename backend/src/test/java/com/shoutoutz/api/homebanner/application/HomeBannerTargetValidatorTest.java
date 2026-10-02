package com.shoutoutz.api.homebanner.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.feed.domain.Feed;
import com.shoutoutz.api.feed.domain.FeedRepository;
import com.shoutoutz.api.homebanner.domain.BannerTargetType;
import com.shoutoutz.api.homebanner.domain.HomeBannerErrorCode;
import com.shoutoutz.api.news.application.NewsQueryRepository;
import com.shoutoutz.api.news.application.dto.NewsDetail;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.Slug;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeBannerTargetValidatorTest {

    @Mock
    private NewsQueryRepository newsQueryRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private FeedRepository feedRepository;

    private HomeBannerTargetValidator validator;

    @BeforeEach
    void setUp() {
        validator = new HomeBannerTargetValidator(
                newsQueryRepository,
                projectRepository,
                feedRepository
        );
    }

    @Test
    void URL_배너는_대상을_검증하지_않는다() {
        assertThat(validator.resolveTargetId(null, null, null)).isNull();

        verifyNoInteractions(newsQueryRepository, projectRepository, feedRepository);
    }

    @Test
    void 공개된_프로젝트는_slug로_지정하면_프로젝트_ID를_돌려준다() {
        when(projectRepository.findIdBySlug(new Slug("loop"))).thenReturn(Optional.of(2L));
        when(projectRepository.existsPublicById(2L)).thenReturn(true);

        assertThat(validator.resolveTargetId(BannerTargetType.PROJECT, null, "loop")).isEqualTo(2L);
    }

    @Test
    void 형식이_틀린_slug는_없는_대상으로_본다() {
        assertTargetNotFound(() -> validator.resolveTargetId(BannerTargetType.PROJECT, null, "Loop!"));

        verifyNoInteractions(projectRepository);
    }

    @Test
    void 없는_slug는_대상으로_지정할_수_없다() {
        when(projectRepository.findIdBySlug(new Slug("loop"))).thenReturn(Optional.empty());

        assertTargetNotFound(() -> validator.resolveTargetId(BannerTargetType.PROJECT, null, "loop"));
    }

    @Test
    void 공개되지_않은_프로젝트는_대상으로_지정할_수_없다() {
        when(projectRepository.findIdBySlug(new Slug("loop"))).thenReturn(Optional.of(2L));
        when(projectRepository.existsPublicById(2L)).thenReturn(false);

        assertTargetNotFound(() -> validator.resolveTargetId(BannerTargetType.PROJECT, null, "loop"));
    }

    @Test
    void 존재하는_소식은_ID를_그대로_돌려준다() {
        when(newsQueryRepository.findDetailById(2L, false))
                .thenReturn(Optional.of(mock(NewsDetail.class)));

        assertThat(validator.resolveTargetId(BannerTargetType.NEWS, 2L, null)).isEqualTo(2L);
    }

    @Test
    void 존재하지_않는_소식은_대상으로_지정할_수_없다() {
        when(newsQueryRepository.findDetailById(2L, false)).thenReturn(Optional.empty());

        assertTargetNotFound(() -> validator.resolveTargetId(BannerTargetType.NEWS, 2L, null));
    }

    @Test
    void 삭제된_피드는_대상으로_지정할_수_없다() {
        when(feedRepository.findActiveById(2L)).thenReturn(Optional.empty());

        assertTargetNotFound(() -> validator.resolveTargetId(BannerTargetType.FEED, 2L, null));
    }

    @Test
    void 존재하는_피드는_ID를_그대로_돌려준다() {
        when(feedRepository.findActiveById(2L)).thenReturn(Optional.of(mock(Feed.class)));

        assertThat(validator.resolveTargetId(BannerTargetType.FEED, 2L, null)).isEqualTo(2L);
    }

    private void assertTargetNotFound(ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(NotFoundException.class, error -> assertThat(error.getErrorCode())
                        .isEqualTo(HomeBannerErrorCode.HOME_BANNER_TARGET_NOT_FOUND));
    }
}
