package com.shoutoutz.api.resourcesearch.application.dto;

import com.shoutoutz.api.resourcesearch.domain.ResourceTopicSide;

public record CatalogTopic(ResourceTopicSide side, ResourceTopic topic) {
}
