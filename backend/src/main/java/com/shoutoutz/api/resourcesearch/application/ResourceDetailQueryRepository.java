package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.ResourceDetail;
import java.util.Optional;

public interface ResourceDetailQueryRepository {

    Optional<ResourceDetail> findById(long resourceId);
}
