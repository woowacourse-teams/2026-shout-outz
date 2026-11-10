package com.shoutoutz.api.projectresource.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.shoutoutz.api.projectresource.domain.ProjectResourceTopics.JobRoleCount;
import com.shoutoutz.api.projectresource.domain.ProjectResourceTopics.TopicCount;
import com.shoutoutz.api.projectresource.domain.ProjectResourceTopics.TopicField;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectResourceTopicsTest {

    private static final List<ResourceTopic> CATALOG = List.of(
            new ResourceTopic("n_plus_one", "N+1 문제", "backend", "데이터"),
            new ResourceTopic("db_index", "인덱스/쿼리 최적화", "backend", "데이터"),
            new ResourceTopic("concurrency", "동시성 제어/락", "backend", "동시성, 성능"),
            new ResourceTopic("auth", "인증/인가", "common", "아키텍처, 설계"),
            new ResourceTopic("react_rendering", "React 렌더링/훅", "frontend", "프론트엔드")
    );

    @Test
    @DisplayName("전체 수와 종류별 수는 직군과 상관없이 이 프로젝트 팀 자료로 센다")
    void countsAllTeamResources() {
        ProjectResourceTopics topics = ProjectResourceTopics.of(CATALOG, List.of(
                new ResourceCount("n_plus_one", "BE", "github-pr", 3),
                new ResourceCount("n_plus_one", null, "blog-post", 1),
                new ResourceCount("auth", "FE", "github-teamdoc", 2),
                new ResourceCount("auth", "ALL", "github-wiki", 1),
                new ResourceCount("react_rendering", "FE", "github-discussion", 1)
        ));

        assertThat(topics.totalCount()).isEqualTo(8);
        assertThat(topics.countOf(ProjectResourceKind.BLOG)).isEqualTo(1);
        assertThat(topics.countOf(ProjectResourceKind.PR)).isEqualTo(3);
        assertThat(topics.countOf(ProjectResourceKind.TEAM_DOC)).isEqualTo(4);
    }

    @Test
    @DisplayName("카탈로그에 없는 주제와 팀 자료가 아닌 원천은 세지 않는다")
    void ignoresUnknownTopicAndNonTeamSource() {
        ProjectResourceTopics topics = ProjectResourceTopics.of(CATALOG, List.of(
                new ResourceCount("n_plus_one", "BE", "github-pr", 2),
                new ResourceCount("removed_topic", "BE", "github-pr", 5),
                new ResourceCount("n_plus_one", null, "youtube-tekotalk", 4)
        ));

        assertThat(topics.totalCount()).isEqualTo(2);
        assertThat(topics.fields(ProjectResourceJobRole.ALL))
                .flatExtracting(TopicField::topics)
                .extracting(TopicCount::topicId)
                .containsExactly("n_plus_one");
    }

    @Test
    @DisplayName("직군 탭은 ALL 이 늘 있고, 그 직군 자료가 있는 직군만 공통 자료까지 더해 센다")
    void countsJobRoleTabs() {
        ProjectResourceTopics topics = ProjectResourceTopics.of(CATALOG, List.of(
                new ResourceCount("n_plus_one", "BE", "github-pr", 3),
                new ResourceCount("auth", "ALL", "github-teamdoc", 2),
                new ResourceCount("auth", "FE", "github-pr", 1),
                new ResourceCount("react_rendering", "FE", "github-pr", 4),
                new ResourceCount("react_rendering", null, "blog-post", 1)
        ));

        assertThat(topics.jobRoleCounts()).containsExactly(
                new JobRoleCount(ProjectResourceJobRole.ALL, 11),
                new JobRoleCount(ProjectResourceJobRole.BE, 5),
                new JobRoleCount(ProjectResourceJobRole.FE, 8)
        );
    }

    @Test
    @DisplayName("그 직군 자료가 다른 직군 주제에만 있어 보일 자료가 없으면 그 직군 탭을 두지 않는다")
    void dropsJobRoleTabWithoutVisibleResources() {
        ProjectResourceTopics topics = ProjectResourceTopics.of(CATALOG, List.of(
                new ResourceCount("n_plus_one", "BE", "github-pr", 3),
                new ResourceCount("n_plus_one", "FE", "github-pr", 1)
        ));

        assertThat(topics.jobRoleCounts()).extracting(JobRoleCount::jobRole)
                .containsExactly(ProjectResourceJobRole.ALL, ProjectResourceJobRole.BE);
    }

    @Test
    @DisplayName("주제를 분야로 묶고, 분야와 주제 모두 자료가 많은 순, 같으면 이름순으로 둔다")
    void groupsTopicsByCategory() {
        ProjectResourceTopics topics = ProjectResourceTopics.of(CATALOG, List.of(
                new ResourceCount("db_index", "BE", "github-pr", 2),
                new ResourceCount("n_plus_one", "BE", "github-pr", 1),
                new ResourceCount("n_plus_one", "BE", "github-teamdoc", 1),
                new ResourceCount("concurrency", "BE", "github-pr", 6),
                new ResourceCount("auth", "BE", "github-pr", 1)
        ));

        assertThat(topics.fields(ProjectResourceJobRole.ALL))
                .extracting(TopicField::name, TopicField::count)
                .containsExactly(
                        tuple("동시성, 성능", 6L),
                        tuple("데이터", 4L),
                        tuple("아키텍처, 설계", 1L)
                );
        assertThat(topics.fields(ProjectResourceJobRole.ALL).get(1).topics()).containsExactly(
                new TopicCount("n_plus_one", "N+1 문제", 2),
                new TopicCount("db_index", "인덱스/쿼리 최적화", 2)
        );
    }

    @Test
    @DisplayName("직군을 고르면 그 직군 주제와 공통 주제에서 그 직군, ALL, 직군 없는 자료만 센다")
    void filtersFieldsByJobRole() {
        ProjectResourceTopics topics = ProjectResourceTopics.of(CATALOG, List.of(
                new ResourceCount("n_plus_one", "BE", "github-pr", 2),
                new ResourceCount("auth", "BE", "github-pr", 1),
                new ResourceCount("auth", "FE", "github-pr", 3),
                new ResourceCount("auth", null, "blog-post", 1),
                new ResourceCount("react_rendering", "FE", "github-pr", 4)
        ));

        assertThat(topics.fields(ProjectResourceJobRole.BE))
                .flatExtracting(TopicField::topics)
                .containsExactlyInAnyOrder(
                        new TopicCount("n_plus_one", "N+1 문제", 2),
                        new TopicCount("auth", "인증/인가", 2)
                );
        assertThat(topics.fields(ProjectResourceJobRole.FE))
                .flatExtracting(TopicField::topics)
                .containsExactlyInAnyOrder(
                        new TopicCount("react_rendering", "React 렌더링/훅", 4),
                        new TopicCount("auth", "인증/인가", 4)
                );
    }

    @Test
    @DisplayName("자료가 없으면 수는 0이고 직군 탭은 ALL 만, 주제 목록은 비어 있다")
    void emptyProject() {
        ProjectResourceTopics topics = ProjectResourceTopics.of(CATALOG, List.of());

        assertThat(topics.totalCount()).isZero();
        assertThat(topics.jobRoleCounts()).containsExactly(new JobRoleCount(ProjectResourceJobRole.ALL, 0));
        assertThat(topics.fields(ProjectResourceJobRole.ALL)).isEmpty();
    }
}
