package com.shoutoutz.api.projectresource.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.shoutoutz.api.projectresource.domain.ProjectTopicCounts.GroupCount;
import com.shoutoutz.api.projectresource.domain.ProjectTopicCounts.TopicCount;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectTopicCountsTest {

    private static final List<ResourceTopic> CATALOG = List.of(
            new ResourceTopic("auth", "인증/인가", "common", "아키텍처, 설계"),
            new ResourceTopic("zero_downtime", "무중단 배포", "common", "인프라, DevOps"),
            new ResourceTopic("concurrency", "동시성 제어/락", "backend", "동시성, 성능"),
            new ResourceTopic("n_plus_one", "N+1 문제", "backend", "데이터"),
            new ResourceTopic("react_rendering", "React 렌더링/훅", "frontend", "프론트엔드"),
            new ResourceTopic("android", "Android", "mobile", "모바일")
    );

    @Test
    @DisplayName("전체 수는 주제가 하나라도 있는 프로젝트 수이고, 묶음 수는 그 묶음 주제를 다룬 프로젝트 수다")
    void countsDistinctProjects() {
        ProjectTopicCounts counts = ProjectTopicCounts.of(CATALOG, List.of(
                new TopicProject("concurrency", 1L),
                new TopicProject("n_plus_one", 1L),
                new TopicProject("concurrency", 2L),
                new TopicProject("auth", 2L),
                new TopicProject("react_rendering", 3L)
        ));

        assertThat(counts.totalCount()).isEqualTo(3);
        assertThat(counts.groups()).extracting(GroupCount::group, GroupCount::projectCount).containsExactly(
                tuple(ProjectTopicGroup.COMMON, 1L),
                tuple(ProjectTopicGroup.BE, 2L),
                tuple(ProjectTopicGroup.FE, 1L)
        );
    }

    @Test
    @DisplayName("묶음은 COMMON, BE, FE, AN 순이고 주제가 없는 묶음은 뺀다")
    void ordersGroupsAndDropsEmpty() {
        ProjectTopicCounts counts = ProjectTopicCounts.of(CATALOG, List.of(
                new TopicProject("android", 1L),
                new TopicProject("concurrency", 2L),
                new TopicProject("auth", 3L)
        ));

        assertThat(counts.groups()).extracting(GroupCount::group)
                .containsExactly(ProjectTopicGroup.COMMON, ProjectTopicGroup.BE, ProjectTopicGroup.AN);
    }

    @Test
    @DisplayName("묶음 안 주제는 프로젝트가 많은 순, 같으면 이름순이다")
    void ordersTopicsByProjectCountThenName() {
        ProjectTopicCounts counts = ProjectTopicCounts.of(CATALOG, List.of(
                new TopicProject("n_plus_one", 1L),
                new TopicProject("concurrency", 2L),
                new TopicProject("concurrency", 3L),
                new TopicProject("n_plus_one", 4L)
        ));

        assertThat(counts.groups().getFirst().topics()).containsExactly(
                new TopicCount("n_plus_one", "N+1 문제", 2),
                new TopicCount("concurrency", "동시성 제어/락", 2)
        );
    }

    @Test
    @DisplayName("카탈로그에 없는 주제와 같은 (주제, 프로젝트)가 겹친 것은 한 번만 센다")
    void ignoresUnknownTopicsAndDuplicates() {
        ProjectTopicCounts counts = ProjectTopicCounts.of(CATALOG, List.of(
                new TopicProject("concurrency", 1L),
                new TopicProject("concurrency", 1L),
                new TopicProject("removed_topic", 2L)
        ));

        assertThat(counts.totalCount()).isEqualTo(1);
        assertThat(counts.groups()).hasSize(1);
        assertThat(counts.groups().getFirst().topics())
                .containsExactly(new TopicCount("concurrency", "동시성 제어/락", 1));
    }

    @Test
    @DisplayName("주제 직군으로 묶음을 고른다")
    void mapsSideToGroup() {
        assertThat(ProjectTopicGroup.fromSide("common")).contains(ProjectTopicGroup.COMMON);
        assertThat(ProjectTopicGroup.fromSide("backend")).contains(ProjectTopicGroup.BE);
        assertThat(ProjectTopicGroup.fromSide("frontend")).contains(ProjectTopicGroup.FE);
        assertThat(ProjectTopicGroup.fromSide("mobile")).contains(ProjectTopicGroup.AN);
        assertThat(ProjectTopicGroup.fromSide("unknown")).isEmpty();
    }

    @Test
    @DisplayName("주제가 있는 프로젝트가 없으면 전체 수는 0이고 묶음은 없다")
    void empty() {
        ProjectTopicCounts counts = ProjectTopicCounts.of(CATALOG, List.of());

        assertThat(counts.totalCount()).isZero();
        assertThat(counts.groups()).isEmpty();
    }
}
