package com.shoutoutz.api.projectresource.presentation.dto.response;

import com.shoutoutz.api.projectresource.domain.ResourceTopic;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCardLogoUrls;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceCardResponse;
import java.util.List;

/**
 * 기술 이야기 탭에서 고른 주제의 자료. 카드는 기술 자료 검색 결과 카드와 같은 모양이다.
 *
 * @param resources          이 팀 자료 전부
 * @param talks              그 주제의 테코톡 전부
 * @param otherTeamResources 같은 주제의 다른 팀 자료. 점수순 상위 20건
 */
public record ProjectTopicResourcesResponse(
        Topic topic,
        List<ResourceCardResponse> resources,
        List<ResourceCardResponse> talks,
        List<ResourceCardResponse> otherTeamResources
) {

    public ProjectTopicResourcesResponse {
        resources = List.copyOf(resources);
        talks = List.copyOf(talks);
        otherTeamResources = List.copyOf(otherTeamResources);
    }

    public static ProjectTopicResourcesResponse of(
            ResourceTopic topic,
            List<ResourceCard> resources,
            List<ResourceCard> talks,
            List<ResourceCard> otherTeamResources,
            ResourceCardLogoUrls logoUrls
    ) {
        return new ProjectTopicResourcesResponse(
                new Topic(topic.topicId(), topic.name()),
                cards(resources, logoUrls),
                cards(talks, logoUrls),
                cards(otherTeamResources, logoUrls)
        );
    }

    private static List<ResourceCardResponse> cards(List<ResourceCard> cards, ResourceCardLogoUrls logoUrls) {
        return cards.stream()
                .map(card -> ResourceCardResponse.from(card, logoUrls))
                .toList();
    }

    public record Topic(String topicId, String name) {
    }
}
