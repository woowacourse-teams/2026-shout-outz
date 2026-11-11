package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.CatalogTopic;
import com.shoutoutz.api.resourcesearch.application.dto.PopularTopic;
import java.util.List;

public interface ResourceTopicQueryRepository {

    List<CatalogTopic> findAllOrderByName();

    List<PopularTopic> findMostCovered(int size);
}
