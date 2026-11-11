package com.shoutoutz.api.projectresource.domain;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 한 프로젝트 기술 이야기 탭의 자료 수, 직군 탭, 분야별 주제 목록.
 * 수는 이 프로젝트의 팀 자료(삭제되지 않았고, 주제가 있고, 받아들인 필요 없어요 제보가 없는 자료)로 센다.
 */
public final class ProjectResourceTopics {

    private static final Comparator<TopicCount> TOPIC_ORDER = Comparator.comparingLong(TopicCount::count)
            .reversed()
            .thenComparing(TopicCount::name);
    private static final Comparator<TopicField> FIELD_ORDER = Comparator.comparingLong(TopicField::count)
            .reversed()
            .thenComparing(TopicField::name);

    private final Map<String, ResourceTopic> topics;
    private final List<ResourceCount> counts;

    private ProjectResourceTopics(Map<String, ResourceTopic> topics, List<ResourceCount> counts) {
        this.topics = topics;
        this.counts = counts;
    }

    /**
     * 카탈로그에 없는 주제의 수와 팀 자료가 아닌 원천(테코톡 등)의 수는 뺀다.
     */
    public static ProjectResourceTopics of(List<ResourceTopic> catalog, List<ResourceCount> counts) {
        Map<String, ResourceTopic> topics = catalog.stream()
                .collect(toMap(ResourceTopic::topicId, Function.identity()));
        List<ResourceCount> teamCounts = counts.stream()
                .filter(count -> topics.containsKey(count.topicId()))
                .filter(count -> ProjectResourceKind.fromSource(count.source()).isPresent())
                .toList();
        return new ProjectResourceTopics(topics, teamCounts);
    }

    /**
     * 직군과 상관없는 이 프로젝트 팀 자료 수
     */
    public long totalCount() {
        return sum(counts.stream());
    }

    /**
     * 직군과 상관없는 종류별 팀 자료 수
     */
    public long countOf(ProjectResourceKind kind) {
        return sum(counts.stream()
                .filter(count -> ProjectResourceKind.fromSource(count.source()).orElseThrow() == kind));
    }

    /**
     * 직군 탭과 탭마다 보이는 자료 수. ALL 은 늘 있고, BE, FE, AN 은 자료 직군이 그 직군인 자료가 있을 때만 있다.
     * 공통 자료는 여러 탭에 함께 들어가므로 탭 수를 더하면 전체보다 클 수 있다.
     */
    public List<JobRoleCount> jobRoleCounts() {
        List<JobRoleCount> jobRoleCounts = new ArrayList<>();
        jobRoleCounts.add(new JobRoleCount(ProjectResourceJobRole.ALL, totalCount()));
        for (ProjectResourceJobRole jobRole : ProjectResourceJobRole.tracks()) {
            long count = sum(visibleTo(jobRole));
            if (hasTrack(jobRole) && count > 0) {
                jobRoleCounts.add(new JobRoleCount(jobRole, count));
            }
        }
        return List.copyOf(jobRoleCounts);
    }

    /**
     * 고른 직군 탭에 보일 주제를 분야로 묶는다. 분야와 주제 모두 자료가 많은 순이고, 같으면 이름순이다.
     */
    public List<TopicField> fields(ProjectResourceJobRole jobRole) {
        Map<String, Long> countByTopic = visibleTo(jobRole)
                .collect(groupingBy(ResourceCount::topicId, LinkedHashMap::new, summingLong(ResourceCount::count)));
        Map<String, List<TopicCount>> topicsByField = new LinkedHashMap<>();
        countByTopic.forEach((topicId, count) -> {
            ResourceTopic topic = topics.get(topicId);
            topicsByField.computeIfAbsent(topic.category(), category -> new ArrayList<>())
                    .add(new TopicCount(topic.topicId(), topic.name(), count));
        });
        return topicsByField.entrySet().stream()
                .map(field -> new TopicField(field.getKey(), field.getValue().stream().sorted(TOPIC_ORDER).toList()))
                .sorted(FIELD_ORDER)
                .toList();
    }

    private Stream<ResourceCount> visibleTo(ProjectResourceJobRole jobRole) {
        return counts.stream()
                .filter(count -> jobRole.includesTopic(topics.get(count.topicId()).side()))
                .filter(count -> jobRole.includesTrack(count.track()));
    }

    private boolean hasTrack(ProjectResourceJobRole jobRole) {
        return counts.stream().anyMatch(count -> jobRole.name().equals(count.track()));
    }

    private static long sum(Stream<ResourceCount> counts) {
        return counts.mapToLong(ResourceCount::count).sum();
    }

    public record JobRoleCount(ProjectResourceJobRole jobRole, long count) {
    }

    public record TopicField(String name, List<TopicCount> topics) {

        public TopicField {
            topics = List.copyOf(topics);
        }

        public long count() {
            return topics.stream().mapToLong(TopicCount::count).sum();
        }
    }

    public record TopicCount(String topicId, String name, long count) {
    }
}
