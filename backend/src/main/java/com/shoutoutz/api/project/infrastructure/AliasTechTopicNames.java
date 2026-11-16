package com.shoutoutz.api.project.infrastructure;

import com.shoutoutz.api.project.domain.NormalizedKeyword;
import com.shoutoutz.api.project.domain.TopicAliases;
import com.shoutoutz.api.resourcesearch.application.TechTopicNames;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AliasTechTopicNames implements TechTopicNames {

    private final TopicAliases topicAliases;

    @Override
    public boolean contains(String keyword) {
        return topicAliases.isAlias(NormalizedKeyword.of(keyword));
    }
}
