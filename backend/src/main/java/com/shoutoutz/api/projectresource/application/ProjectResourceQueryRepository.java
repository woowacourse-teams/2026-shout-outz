package com.shoutoutz.api.projectresource.application;

import com.shoutoutz.api.projectresource.domain.ProjectResourceJobRole;
import com.shoutoutz.api.projectresource.domain.ResourceCount;
import com.shoutoutz.api.projectresource.domain.ResourceTopic;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import java.util.List;
import java.util.Optional;

/**
 * 프로젝트 기술 이야기 조회. 삭제된 자료는 읽지 않고, 관리자가 받아들인 필요 없어요 제보가 있는 자료는
 * 이 팀 자료와 다른 팀 자료에서 뺀다.
 */
public interface ProjectResourceQueryRepository {

    List<ResourceTopic> findTopics();

    Optional<ResourceTopic> findTopic(String topicId);

    /**
     * 이 프로젝트 팀 자료를 주제, 자료 직군, 원천별로 센다. 주제가 없는 자료는 세지 않는다.
     */
    List<ResourceCount> countTeamResources(long projectId);

    /**
     * 이 프로젝트의 그 주제 팀 자료 전부. 블로그 먼저, 그다음 점수 높은 순, 같으면 최신순이다.
     */
    List<ResourceCard> findTeamResources(long projectId, String topicId, ProjectResourceJobRole jobRole);

    /**
     * 그 주제의 테코톡 전부. 최신 발표순이다.
     */
    List<ResourceCard> findTalks(String topicId);

    /**
     * 그 주제의 다른 팀 자료. 공개된 다른 프로젝트의 자료와 프로젝트에 이어지지 않은 자료를 점수 높은 순으로 limit 건까지 읽는다.
     */
    List<ResourceCard> findOtherTeamResources(long projectId, String topicId, ProjectResourceJobRole jobRole, int limit);
}
