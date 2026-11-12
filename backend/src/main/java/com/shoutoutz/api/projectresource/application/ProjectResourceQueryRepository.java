package com.shoutoutz.api.projectresource.application;

import com.shoutoutz.api.projectresource.domain.CareerCard;
import com.shoutoutz.api.projectresource.domain.ProjectResourceJobRole;
import com.shoutoutz.api.projectresource.domain.ResourceCount;
import com.shoutoutz.api.projectresource.domain.ResourceTopic;
import com.shoutoutz.api.projectresource.domain.TopicProject;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 프로젝트 기술 이야기와 전체 경험 조회. 삭제된 자료는 읽지 않고, 관리자가 받아들인 필요 없어요 제보가 있는 자료는
 * 이 팀 자료, 다른 팀 자료, 경력 카드 근거에서 뺀다.
 */
public interface ProjectResourceQueryRepository {

    List<ResourceTopic> findTopics();

    Optional<ResourceTopic> findTopic(String topicId);

    /**
     * 이 프로젝트 팀 자료를 주제, 자료 직군, 원천별로 센다. 주제가 없는 자료는 세지 않는다.
     */
    List<ResourceCount> countTeamResources(long projectId);

    /**
     * 공개된 프로젝트마다 팀 자료를 하나 이상 가진 주제. 같은 (주제, 프로젝트)는 한 번만 준다.
     */
    List<TopicProject> findTopicProjects();

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

    /**
     * 이 프로젝트의 삭제되지 않은 경력 카드. 직군 BE, FE, AN 순이고 직군 안에서는 sort_order 순이다.
     */
    List<CareerCard> findCareerCards(long projectId);

    /**
     * 카드별 근거 자료를 source_order 순으로 읽는다. 삭제된 경험과 자료, 받아들인 필요 없어요 제보 자료는 뺀다.
     * 한 문서의 경험 여러 개가 같은 카드에 묶여도 그 문서는 한 번만 담는다.
     * 근거가 하나도 남지 않은 카드는 결과에 없다.
     */
    Map<Long, List<ResourceCard>> findCareerCardSources(Collection<Long> cardIds);
}
