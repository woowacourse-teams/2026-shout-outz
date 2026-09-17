package com.shoutoutz.api.homebanner.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.feed.domain.Feed;
import com.shoutoutz.api.feed.domain.FeedRepository;
import com.shoutoutz.api.homebanner.domain.BannerTargetType;
import com.shoutoutz.api.news.application.NewsQueryRepository;
import com.shoutoutz.api.news.application.dto.NewsDetail;
import com.shoutoutz.api.project.domain.ProjectRepository;
import java.util.Optional;
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
        assertThatCode(() -> validator.validate(null, null)).doesNotThrowAnyException();

        verifyNoInteractions(newsQueryRepository, projectRepository, feedRepository);
    }

    @Test
    void 공개된_프로젝트를_대상으로_지정할_수_있다() {
        when(projectRepository.existsPublicById(2L)).thenReturn(true);

        assertThatCode(() -> validator.validate(BannerTargetType.PROJECT, 2L))
                .doesNotThrowAnyException();
    }

    @Test
    void 존재하는_소식을_대상으로_지정할_수_있다() {
        when(newsQueryRepository.findDetailById(2L, false))
                .thenReturn(Optional.of(mock(NewsDetail.class)));

        assertThatCode(() -> validator.validate(BannerTargetType.NEWS, 2L))
                .doesNotThrowAnyException();
    }

    @Test
    void 존재하지_않는_소식은_대상으로_지정할_수_없다() {
        when(newsQueryRepository.findDetailById(2L, false)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate(BannerTargetType.NEWS, 2L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 삭제된_피드는_대상으로_지정할_수_없다() {
        when(feedRepository.findActiveById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate(BannerTargetType.FEED, 2L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 존재하는_피드를_대상으로_지정할_수_있다() {
        when(feedRepository.findActiveById(2L)).thenReturn(Optional.of(mock(Feed.class)));

        assertThatCode(() -> validator.validate(BannerTargetType.FEED, 2L))
                .doesNotThrowAnyException();
    }
}
