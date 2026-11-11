package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.shoutoutz.api.resourcesearch.application.dto.CatalogTopic;
import com.shoutoutz.api.resourcesearch.application.dto.PopularTopic;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceTopic;
import com.shoutoutz.api.resourcesearch.domain.ResourceTopicSide;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceTopicPopularRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.PopularTopicResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceTopicResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceTopicSideResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceTopicQueryServiceTest {

    @Mock
    private ResourceTopicQueryRepository resourceTopicQueryRepository;

    @InjectMocks
    private ResourceTopicQueryService resourceTopicQueryService;

    @Test
    @DisplayName("주제를 분야별로 묶고 분야 안에서는 저장소가 준 이름 순서를 지킨다")
    void groupsTopicsBySideKeepingNameOrder() {
        given(resourceTopicQueryRepository.findAllOrderByName()).willReturn(List.of(
                topic(ResourceTopicSide.COMMON, "ci_cd", "CI/CD"),
                topic(ResourceTopicSide.BACKEND, "jpa", "JPA/Hibernate"),
                topic(ResourceTopicSide.FRONTEND, "react", "React 렌더링/훅"),
                topic(ResourceTopicSide.BACKEND, "concurrency", "동시성 제어/락"),
                topic(ResourceTopicSide.MOBILE, "android", "Android")));

        List<ResourceTopicSideResponse> sides = resourceTopicQueryService.findAllBySide();

        assertThat(sides).extracting(ResourceTopicSideResponse::side).containsExactly(
                ResourceTopicSide.COMMON, ResourceTopicSide.BACKEND, ResourceTopicSide.FRONTEND, ResourceTopicSide.MOBILE);
        assertThat(sides.get(1).topics()).extracting(ResourceTopicResponse::name)
                .containsExactly("JPA/Hibernate", "동시성 제어/락");
    }

    @Test
    @DisplayName("주제가 없는 분야도 빈 목록으로 준다")
    void keepsSideWithoutTopics() {
        given(resourceTopicQueryRepository.findAllOrderByName()).willReturn(List.of(
                topic(ResourceTopicSide.BACKEND, "jpa", "JPA/Hibernate")));

        List<ResourceTopicSideResponse> sides = resourceTopicQueryService.findAllBySide();

        assertThat(sides).hasSize(4);
        assertThat(sides.get(3).topics()).isEmpty();
    }

    @Test
    @DisplayName("많이 다룬 주제를 요청한 개수만큼 자료 수와 함께 준다")
    void findsPopularTopicsWithRequestedSize() {
        given(resourceTopicQueryRepository.findMostCovered(3)).willReturn(List.of(
                new PopularTopic(new ResourceTopic("ci_cd", "CI/CD"), 42)));

        List<PopularTopicResponse> topics = resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(3));

        assertThat(topics).containsExactly(new PopularTopicResponse("ci_cd", "CI/CD", 42));
    }

    @Test
    @DisplayName("개수를 생략하면 많이 다룬 주제 10개를 찾는다")
    void usesDefaultPopularSize() {
        given(resourceTopicQueryRepository.findMostCovered(10)).willReturn(List.of());

        List<PopularTopicResponse> topics = resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(null));

        assertThat(topics).isEmpty();
    }

    private CatalogTopic topic(ResourceTopicSide side, String topicId, String name) {
        return new CatalogTopic(side, new ResourceTopic(topicId, name));
    }
}
