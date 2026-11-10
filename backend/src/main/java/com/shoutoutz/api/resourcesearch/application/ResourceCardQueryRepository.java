package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import java.util.List;

public interface ResourceCardQueryRepository {

    List<ResourceCard> findByIds(List<Long> resourceIds);
}
