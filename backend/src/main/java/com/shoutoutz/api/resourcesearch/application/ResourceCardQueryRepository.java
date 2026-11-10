package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import java.util.List;

public interface ResourceCardQueryRepository {

    List<ResourceCard> findByIds(List<Long> resourceIds);

    List<ResourceCard> findRecent(ResourceSection kind, int size);
}
