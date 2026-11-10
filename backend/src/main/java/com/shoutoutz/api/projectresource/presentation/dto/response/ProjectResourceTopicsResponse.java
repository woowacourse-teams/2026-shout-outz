package com.shoutoutz.api.projectresource.presentation.dto.response;

import com.shoutoutz.api.projectresource.domain.ProjectResourceJobRole;
import com.shoutoutz.api.projectresource.domain.ProjectResourceKind;
import com.shoutoutz.api.projectresource.domain.ProjectResourceTopics;
import java.util.List;

/**
 * 기술 이야기 탭의 머리 수, 직군 탭, 왼쪽 주제 목록
 *
 * @param totalCount 직군과 상관없는 이 프로젝트 자료 수
 * @param kindCounts 직군과 상관없는 종류별 자료 수
 * @param jobRoles   직군 탭과 탭마다 자료 수
 * @param fields     고른 직군 탭에 보일 주제를 분야로 묶은 것
 */
public record ProjectResourceTopicsResponse(
        long totalCount,
        KindCounts kindCounts,
        List<JobRoleCount> jobRoles,
        List<Field> fields
) {

    public ProjectResourceTopicsResponse {
        jobRoles = List.copyOf(jobRoles);
        fields = List.copyOf(fields);
    }

    public static ProjectResourceTopicsResponse of(ProjectResourceTopics topics, ProjectResourceJobRole jobRole) {
        return new ProjectResourceTopicsResponse(
                topics.totalCount(),
                new KindCounts(
                        topics.countOf(ProjectResourceKind.BLOG),
                        topics.countOf(ProjectResourceKind.PR),
                        topics.countOf(ProjectResourceKind.TEAM_DOC)
                ),
                topics.jobRoleCounts().stream()
                        .map(count -> new JobRoleCount(count.jobRole(), count.count()))
                        .toList(),
                topics.fields(jobRole).stream()
                        .map(field -> new Field(
                                field.name(),
                                field.topics().stream()
                                        .map(topic -> new Topic(topic.topicId(), topic.name(), topic.count()))
                                        .toList()
                        ))
                        .toList()
        );
    }

    /**
     * @param teamDoc 레포 문서, Wiki, Discussion 을 합한 수
     */
    public record KindCounts(long blog, long pr, long teamDoc) {
    }

    public record JobRoleCount(ProjectResourceJobRole jobRole, long count) {
    }

    public record Field(String name, List<Topic> topics) {

        public Field {
            topics = List.copyOf(topics);
        }
    }

    public record Topic(String topicId, String name, long count) {
    }
}
