package com.shoutoutz.api.projectresource.presentation.dto.response;

import com.shoutoutz.api.projectresource.domain.ProjectTopicCounts;
import com.shoutoutz.api.projectresource.domain.ProjectTopicGroup;
import java.util.List;

/**
 * 탐색 화면 프로젝트 탭의 기술 주제 목록. 모양은 프론트 #607 목업과 같고 주제마다 topicId 를 더했다.
 *
 * @param totalCount 주제가 하나라도 있는 공개 프로젝트 수
 * @param groups     직군 묶음. COMMON, BE, FE, AN 순이고 주제가 없는 묶음은 없다
 */
public record ProjectTopicsResponse(long totalCount, List<Group> groups) {

    public ProjectTopicsResponse {
        groups = List.copyOf(groups);
    }

    public static ProjectTopicsResponse from(ProjectTopicCounts counts) {
        return new ProjectTopicsResponse(
                counts.totalCount(),
                counts.groups().stream()
                        .map(group -> new Group(
                                group.group(),
                                group.group().displayName(),
                                group.group().shortName(),
                                group.projectCount(),
                                group.topics().stream()
                                        .map(topic -> new Topic(topic.topicId(), topic.name(), topic.projectCount()))
                                        .toList()
                        ))
                        .toList()
        );
    }

    /**
     * @param projectCount 그 묶음 주제를 하나라도 다룬 프로젝트 수
     * @param topics       프로젝트가 많은 순, 같으면 이름순
     */
    public record Group(
            ProjectTopicGroup code,
            String name,
            String shortName,
            long projectCount,
            List<Topic> topics
    ) {

        public Group {
            topics = List.copyOf(topics);
        }
    }

    /**
     * @param topicId 프로젝트 목록 조회의 topicId 조건으로 쓴다
     */
    public record Topic(String topicId, String name, long projectCount) {
    }
}
