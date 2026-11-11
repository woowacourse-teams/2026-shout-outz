package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.CatalogTopic;
import com.shoutoutz.api.resourcesearch.domain.ResourceTopicSide;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceTopicPopularRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.PopularTopicResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceTopicResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceTopicSideResponse;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceTopicQueryService {

    private final ResourceTopicQueryRepository resourceTopicQueryRepository;

    public List<ResourceTopicSideResponse> findAllBySide() {
        Map<ResourceTopicSide, List<ResourceTopicResponse>> topicsBySide = groupBySide(
                resourceTopicQueryRepository.findAllOrderByName());
        return Arrays.stream(ResourceTopicSide.values())
                .map(side -> new ResourceTopicSideResponse(side, topicsBySide.getOrDefault(side, List.of())))
                .toList();
    }

    public List<PopularTopicResponse> findPopular(ResourceTopicPopularRequest request) {
        return resourceTopicQueryRepository.findMostCovered(request.resolvedSize()).stream()
                .map(PopularTopicResponse::from)
                .toList();
    }

    private Map<ResourceTopicSide, List<ResourceTopicResponse>> groupBySide(List<CatalogTopic> topics) {
        return topics.stream().collect(Collectors.groupingBy(
                CatalogTopic::side,
                () -> new EnumMap<>(ResourceTopicSide.class),
                Collectors.mapping(topic -> ResourceTopicResponse.from(topic.topic()), Collectors.toList())
        ));
    }
}
