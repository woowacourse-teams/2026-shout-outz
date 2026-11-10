package com.shoutoutz.api.projectresource.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.projectresource.application.ProjectResourceQueryRepository;
import com.shoutoutz.api.projectresource.domain.ProjectResourceJobRole;
import com.shoutoutz.api.projectresource.domain.ResourceCount;
import com.shoutoutz.api.projectresource.domain.ResourceTopic;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ProjectResourceQueryRepositoryIntegrationTest {

    private static final String TOPIC = "concurrency";
    private static final String OTHER_TOPIC = "n_plus_one";

    @Autowired
    private ProjectResourceQueryRepository projectResourceQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ProjectResourceFixtures fixtures;
    private long projectId;

    @BeforeEach
    void setUp() {
        fixtures = new ProjectResourceFixtures(jdbcTemplate);
        projectId = fixtures.publicProject("샤라웃");
    }

    @Test
    @DisplayName("기술 주제 77개 모두 분야가 있고, 주제 id 로 주제 하나를 찾는다")
    void findsTopicsWithCategory() {
        List<ResourceTopic> topics = projectResourceQueryRepository.findTopics();

        assertThat(topics).hasSize(77);
        assertThat(topics).allSatisfy(topic -> assertThat(topic.category()).isNotBlank());
        assertThat(projectResourceQueryRepository.findTopic("n_plus_one"))
                .contains(new ResourceTopic("n_plus_one", "N+1 문제", "backend", "데이터"));
        assertThat(projectResourceQueryRepository.findTopic("unknown")).isEmpty();
    }

    @Test
    @DisplayName("이 프로젝트 팀 자료를 주제, 직군, 원천별로 세고, 내려간 자료와 받아들인 필요 없어요 제보 자료는 뺀다")
    void countsTeamResources() {
        long otherProjectId = fixtures.publicProject("다른 팀");
        fixtures.document("github-pr", "pr-1").project(projectId).topic(TOPIC).track("BE").insert();
        fixtures.document("github-pr", "pr-2").project(projectId).topic(TOPIC).track("BE").insert();
        fixtures.document("blog-post", "blog-1").project(projectId).topic(TOPIC).insert();
        fixtures.document("github-wiki", "wiki-1").project(projectId).topic(OTHER_TOPIC).track("ALL").insert();
        fixtures.document("github-pr", "rejected").project(projectId).topic(TOPIC).track("BE").insert();
        fixtures.removalReport(projectId, "github-pr", "rejected", "REJECTED");
        fixtures.document("github-pr", "accepted").project(projectId).topic(TOPIC).track("BE").insert();
        fixtures.removalReport(projectId, "github-pr", "accepted", "ACCEPTED");
        fixtures.document("github-pr", "deleted").project(projectId).topic(TOPIC).track("BE").deleted().insert();
        fixtures.document("github-pr", "no-topic").project(projectId).track("BE").insert();
        fixtures.document("github-pr", "other").project(otherProjectId).topic(TOPIC).track("BE").insert();

        List<ResourceCount> counts = projectResourceQueryRepository.countTeamResources(projectId);

        assertThat(counts).containsExactlyInAnyOrder(
                new ResourceCount(TOPIC, "BE", "github-pr", 3),
                new ResourceCount(TOPIC, null, "blog-post", 1),
                new ResourceCount(OTHER_TOPIC, "ALL", "github-wiki", 1)
        );
    }

    @Test
    @DisplayName("이 팀 자료는 블로그 먼저, 그다음 점수 높은 순, 같으면 최신순으로 프로젝트 팀 정보와 함께 읽는다")
    void findsTeamResourcesInOrder() {
        long mediaId = fixtures.media();
        long projectWithLogo = fixtures.project("로고 팀", "APPROVED", false, mediaId);
        long olderPr = fixtures.document("github-pr", "pr-old").project(projectWithLogo).topic(TOPIC).track("BE")
                .score(80).publishedAt("2026-09-01T00:00:00Z").insert();
        long newerPr = fixtures.document("github-pr", "pr-new").project(projectWithLogo).topic(TOPIC).track("BE")
                .score(80).publishedAt("2026-10-01T00:00:00Z").insert();
        long bestDoc = fixtures.document("github-teamdoc", "doc-best").project(projectWithLogo).topic(TOPIC)
                .score(95).publishedAt("2026-08-01T00:00:00Z").insert();
        long blog = fixtures.document("blog-post", "blog").project(projectWithLogo).topic(TOPIC)
                .publishedAt("2026-07-01T00:00:00Z").extra("{\"platform\": \"tistory\"}").insert();
        fixtures.document("github-pr", "other-topic").project(projectWithLogo).topic(OTHER_TOPIC).score(99).insert();
        fixtures.document("github-pr", "removed").project(projectWithLogo).topic(TOPIC).score(99).insert();
        fixtures.removalReport(projectWithLogo, "github-pr", "removed", "ACCEPTED");
        fixtures.document("github-pr", "open-report").project(projectWithLogo).topic(TOPIC).score(10).insert();
        fixtures.removalReport(projectWithLogo, "github-pr", "open-report", "OPEN");

        List<ResourceCard> cards = projectResourceQueryRepository.findTeamResources(
                projectWithLogo,
                TOPIC,
                ProjectResourceJobRole.ALL
        );

        assertThat(cards).extracting(ResourceCard::resourceId)
                .containsExactly(blog, bestDoc, newerPr, olderPr, idOf("open-report"));
        ResourceCard blogCard = cards.getFirst();
        assertThat(blogCard.platform()).isEqualTo("tistory");
        assertThat(blogCard.summary()).isEqualTo("한 줄 blog");
        assertThat(blogCard.publishedAt()).isEqualTo(Instant.parse("2026-07-01T00:00:00Z"));
        assertThat(blogCard.team()).isEqualTo(new ResourceCard.Team(
                "로고 팀",
                fixtures.slugOf(projectWithLogo),
                mediaId
        ));
        assertThat(cards.get(1).platform()).isNull();
    }

    @Test
    @DisplayName("직군을 고르면 이 팀 자료 중 그 직군, ALL, 직군 없는 자료만 읽는다")
    void filtersTeamResourcesByTrack() {
        long backend = fixtures.document("github-pr", "be").project(projectId).topic(TOPIC).track("BE").insert();
        long common = fixtures.document("github-pr", "all").project(projectId).topic(TOPIC).track("ALL").insert();
        long blog = fixtures.document("blog-post", "none").project(projectId).topic(TOPIC).insert();
        fixtures.document("github-pr", "fe").project(projectId).topic(TOPIC).track("FE").insert();

        List<ResourceCard> cards = projectResourceQueryRepository.findTeamResources(
                projectId,
                TOPIC,
                ProjectResourceJobRole.BE
        );

        assertThat(cards).extracting(ResourceCard::resourceId).containsExactlyInAnyOrder(backend, common, blog);
    }

    @Test
    @DisplayName("테코톡은 그 주제의 것을 최신 발표순으로 모두 읽고, 팀은 비워 둔다")
    void findsTalksLatestFirst() {
        long older = fixtures.document("youtube-tekotalk", "yt:old").topic(TOPIC)
                .publishedAt("2025-01-01T00:00:00Z").insert();
        long newer = fixtures.document("youtube-tekotalk", "yt:new").topic(TOPIC)
                .publishedAt("2025-09-05T09:00:00Z").insert();
        long undated = fixtures.document("youtube-tekotalk", "yt:undated").topic(TOPIC).insert();
        fixtures.document("youtube-tekotalk", "yt:other").topic(OTHER_TOPIC).insert();
        fixtures.document("youtube-tekotalk", "yt:deleted").topic(TOPIC).deleted().insert();

        List<ResourceCard> talks = projectResourceQueryRepository.findTalks(TOPIC);

        assertThat(talks).extracting(ResourceCard::resourceId).containsExactly(newer, older, undated);
        assertThat(talks).allSatisfy(talk -> assertThat(talk.team()).isNull());
    }

    @Test
    @DisplayName("다른 팀 자료는 공개된 다른 프로젝트와 프로젝트 없는 자료를 점수순(점수 없으면 85점)으로 limit 건까지 읽는다")
    void findsOtherTeamResources() {
        long otherProjectId = fixtures.publicProject("페스타북");
        long pendingProjectId = fixtures.project("심사 중", "PENDING", false, null);
        long deletedProjectId = fixtures.project("삭제된 팀", "APPROVED", true, null);
        long highPr = fixtures.document("github-pr", "other-high").project(otherProjectId).topic(TOPIC).score(90)
                .insert();
        long unregisteredBlog = fixtures.document("blog-post", "unregistered").topic(TOPIC)
                .publishedAt("2025-10-06T03:00:00Z").extra("{\"platform\": \"velog\", \"project_name\": \"등록 전 팀\"}")
                .insert();
        long lowDoc = fixtures.document("github-teamdoc", "other-low").project(otherProjectId).topic(TOPIC).score(70)
                .insert();
        fixtures.document("github-pr", "mine").project(projectId).topic(TOPIC).score(100).insert();
        fixtures.document("github-pr", "pending").project(pendingProjectId).topic(TOPIC).score(100).insert();
        fixtures.document("github-pr", "deleted-project").project(deletedProjectId).topic(TOPIC).score(100).insert();
        fixtures.document("github-pr", "removed").project(otherProjectId).topic(TOPIC).score(100).insert();
        fixtures.removalReport(otherProjectId, "github-pr", "removed", "ACCEPTED");
        fixtures.document("youtube-tekotalk", "yt:talk").topic(TOPIC).insert();

        List<ResourceCard> all = projectResourceQueryRepository.findOtherTeamResources(
                projectId,
                TOPIC,
                ProjectResourceJobRole.ALL,
                20
        );
        List<ResourceCard> limited = projectResourceQueryRepository.findOtherTeamResources(
                projectId,
                TOPIC,
                ProjectResourceJobRole.ALL,
                2
        );

        assertThat(all).extracting(ResourceCard::resourceId).containsExactly(highPr, unregisteredBlog, lowDoc);
        assertThat(limited).extracting(ResourceCard::resourceId).containsExactly(highPr, unregisteredBlog);
        assertThat(all.get(0).team()).isEqualTo(new ResourceCard.Team("페스타북", fixtures.slugOf(otherProjectId), null));
        assertThat(all.get(1).team()).isEqualTo(new ResourceCard.Team("등록 전 팀", null, null));
        assertThat(all.get(1).platform()).isEqualTo("velog");
    }

    @Test
    @DisplayName("직군을 고르면 다른 팀 자료도 그 직군, ALL, 직군 없는 자료만 읽는다")
    void filtersOtherTeamResourcesByTrack() {
        long otherProjectId = fixtures.publicProject("다른 팀");
        long backend = fixtures.document("github-pr", "be").project(otherProjectId).topic(TOPIC).track("BE").insert();
        long blog = fixtures.document("blog-post", "none").project(otherProjectId).topic(TOPIC).insert();
        fixtures.document("github-pr", "fe").project(otherProjectId).topic(TOPIC).track("FE").insert();

        List<ResourceCard> cards = projectResourceQueryRepository.findOtherTeamResources(
                projectId,
                TOPIC,
                ProjectResourceJobRole.BE,
                20
        );

        assertThat(cards).extracting(ResourceCard::resourceId).containsExactlyInAnyOrder(backend, blog);
    }

    private long idOf(String externalKey) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM resource_documents WHERE external_key = ?",
                Long.class,
                externalKey
        );
    }
}
