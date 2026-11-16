package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;

public interface ResourceCardQueryRepository {

    List<ResourceCard> findByIds(List<Long> resourceIds);

    List<ResourceCard> findRecent(ResourceSection kind, int size);

    /**
     * 공개 프로젝트들의 그 섹션 자료를 최신순으로 섹션 상한까지 준다. 발행 시각이 없는 자료는 뒤에 둔다.
     *
     * @param track null 이면 트랙으로 거르지 않는다. 있으면 그 트랙과 공통, 트랙 미분류 자료만 준다
     */
    List<ResourceCard> findLatestByProjects(List<Long> projectIds, ResourceSection section, ResourceTrack track);
}
