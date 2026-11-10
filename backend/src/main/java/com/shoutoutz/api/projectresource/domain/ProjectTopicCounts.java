package com.shoutoutz.api.projectresource.domain;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * 탐색 화면 프로젝트 탭의 기술 주제 목록. 주제마다 그 주제의 팀 자료를 가진 공개 프로젝트 수를 센다.
 * 수는 검색어, 기수, 기술 스택과 상관없는 전체 기준이다.
 */
public final class ProjectTopicCounts {

    private static final Comparator<TopicCount> TOPIC_ORDER = Comparator.comparingLong(TopicCount::projectCount)
            .reversed()
            .thenComparing(TopicCount::name);

    private final Map<String, ResourceTopic> topics;
    private final List<TopicProject> topicProjects;

    private ProjectTopicCounts(Map<String, ResourceTopic> topics, List<TopicProject> topicProjects) {
        this.topics = topics;
        this.topicProjects = topicProjects;
    }

    /**
     * 카탈로그에 없는 주제와 묶음을 알 수 없는 주제는 세지 않는다.
     */
    public static ProjectTopicCounts of(List<ResourceTopic> catalog, List<TopicProject> topicProjects) {
        Map<String, ResourceTopic> topics = catalog.stream()
                .collect(toMap(ResourceTopic::topicId, Function.identity()));
        List<TopicProject> known = topicProjects.stream()
                .filter(topicProject -> topics.containsKey(topicProject.topicId()))
                .filter(topicProject -> groupOf(topics.get(topicProject.topicId())).isPresent())
                .distinct()
                .toList();
        return new ProjectTopicCounts(topics, known);
    }

    /**
     * 주제가 하나라도 있는 프로젝트 수
     */
    public long totalCount() {
        return projectIds(topicProjects).size();
    }

    /**
     * 묶음별 주제 목록. COMMON, BE, FE, AN 순이고 주제가 없는 묶음은 뺀다.
     * 주제는 프로젝트가 많은 순, 같으면 이름순이다.
     */
    public List<GroupCount> groups() {
        Map<ProjectTopicGroup, List<TopicProject>> byGroup = new HashMap<>();
        for (TopicProject topicProject : topicProjects) {
            ProjectTopicGroup group = groupOf(topics.get(topicProject.topicId())).orElseThrow();
            byGroup.computeIfAbsent(group, key -> new ArrayList<>()).add(topicProject);
        }
        return Arrays.stream(ProjectTopicGroup.values())
                .filter(byGroup::containsKey)
                .map(group -> new GroupCount(
                        group,
                        projectIds(byGroup.get(group)).size(),
                        topicCounts(byGroup.get(group))
                ))
                .toList();
    }

    private List<TopicCount> topicCounts(List<TopicProject> topicProjects) {
        Map<String, Long> projectCountByTopic = new HashMap<>();
        for (TopicProject topicProject : topicProjects) {
            projectCountByTopic.merge(topicProject.topicId(), 1L, Long::sum);
        }
        return projectCountByTopic.entrySet().stream()
                .map(entry -> new TopicCount(entry.getKey(), topics.get(entry.getKey()).name(), entry.getValue()))
                .sorted(TOPIC_ORDER)
                .toList();
    }

    private static Optional<ProjectTopicGroup> groupOf(ResourceTopic topic) {
        return ProjectTopicGroup.fromSide(topic.side());
    }

    private static Set<Long> projectIds(List<TopicProject> topicProjects) {
        return topicProjects.stream()
                .map(TopicProject::projectId)
                .collect(toSet());
    }

    /**
     * @param projectCount 그 묶음 주제를 하나라도 다룬 프로젝트 수
     */
    public record GroupCount(ProjectTopicGroup group, long projectCount, List<TopicCount> topics) {

        public GroupCount {
            topics = List.copyOf(topics);
        }
    }

    public record TopicCount(String topicId, String name, long projectCount) {
    }
}
