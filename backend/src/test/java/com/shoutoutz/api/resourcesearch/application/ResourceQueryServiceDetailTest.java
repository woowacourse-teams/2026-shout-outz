package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceDetail;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceExperience;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceTopic;
import com.shoutoutz.api.resourcesearch.domain.ResourceSearchErrorCode;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceDetailResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceQueryServiceDetailTest {

    @Mock
    private ResourceDetailQueryRepository resourceDetailQueryRepository;

    @InjectMocks
    private ResourceQueryService resourceQueryService;

    @Test
    @DisplayName("자료 상세를 응답으로 바꿔 준다")
    void findsDetail() {
        ResourceCard card = new ResourceCard(101L, "github-pr", "제목", "https://example.com", "소개", null, null, null);
        given(resourceDetailQueryRepository.findById(101L)).willReturn(Optional.of(new ResourceDetail(
                card, "problem_solving", new ResourceTopic("concurrency", "동시성 제어/락"),
                List.of("배울 점"), List.of(new ResourceExperience("문제", null, "해결", null, List.of("Redis"))))));

        ResourceDetailResponse response = resourceQueryService.findDetail(101L);

        assertThat(response.resourceId()).isEqualTo(101L);
        assertThat(response.contentType()).isEqualTo("problem_solving");
        assertThat(response.topic().topicId()).isEqualTo("concurrency");
        assertThat(response.keyPoints()).containsExactly("배울 점");
        assertThat(response.experiences()).singleElement()
                .satisfies(experience -> assertThat(experience.techs()).containsExactly("Redis"));
    }

    @Test
    @DisplayName("없거나 삭제된 자료면 404 예외를 던진다")
    void throwsWhenMissing() {
        given(resourceDetailQueryRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> resourceQueryService.findDetail(404L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(ResourceSearchErrorCode.RESOURCE_NOT_FOUND.getMessage());
    }
}
