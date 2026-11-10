package com.shoutoutz.api.projectresource.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.projectresource.application.ProjectResourceQueryRepository;
import com.shoutoutz.api.projectresource.domain.CareerCard;
import com.shoutoutz.api.projectresource.domain.ProjectResourceJobRole;
import com.shoutoutz.api.projectresource.domain.ResourceCount;
import com.shoutoutz.api.projectresource.domain.ResourceTopic;
import com.shoutoutz.api.projectresource.domain.TopicProject;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Test
    @DisplayName("공개된 프로젝트마다 팀 자료가 있는 주제를 한 번씩 읽고, 내려간 자료, 받아들인 제보 자료, 테코톡, 공개되지 않은 프로젝트는 뺀다")
    void findsTopicProjects() {
        long otherProjectId = fixtures.publicProject("다른 팀");
        long pendingProjectId = fixtures.project("심사 중", "PENDING", false, null);
        fixtures.document("github-pr", "pr-1").project(projectId).topic(TOPIC).insert();
        fixtures.document("github-pr", "pr-2").project(projectId).topic(TOPIC).insert();
        fixtures.document("blog-post", "blog").project(otherProjectId).topic(OTHER_TOPIC).insert();
        fixtures.document("github-pr", "deleted").project(otherProjectId).topic(TOPIC).deleted().insert();
        fixtures.document("github-pr", "removed").project(otherProjectId).topic("auth").insert();
        fixtures.removalReport(otherProjectId, "github-pr", "removed", "ACCEPTED");
        fixtures.document("youtube-tekotalk", "yt:talk").project(otherProjectId).topic("auth").insert();
        fixtures.document("github-pr", "pending").project(pendingProjectId).topic(TOPIC).insert();
        Set<Long> projectIds = Set.of(projectId, otherProjectId, pendingProjectId);

        List<TopicProject> topicProjects = projectResourceQueryRepository.findTopicProjects().stream()
                .filter(topicProject -> projectIds.contains(topicProject.projectId()))
                .toList();

        assertThat(topicProjects).containsExactlyInAnyOrder(
                new TopicProject(TOPIC, projectId),
                new TopicProject(OTHER_TOPIC, otherProjectId)
        );
    }

    @Test
    @DisplayName("경력 카드는 직군 BE, FE, AN 순, 직군 안에서는 sort_order 순으로 읽고, 삭제된 카드와 다른 프로젝트 카드는 뺀다")
    void findsCareerCardsInJobRoleAndSortOrder() {
        long otherProjectId = fixtures.publicProject("다른 팀");
        long frontend = fixtures.careerCard(projectId, "FE", "build", 0, false);
        long backendSecond = fixtures.careerCard(projectId, "BE", "problem_solving", 1, false);
        long backendFirst = fixtures.careerCard(projectId, "BE", "tech_decision", 0, false);
        long android = fixtures.careerCard(projectId, "AN", "build", 0, false);
        fixtures.careerCard(projectId, "BE", "build", 2, true);
        fixtures.careerCard(otherProjectId, "BE", "build", 0, false);

        List<CareerCard> cards = projectResourceQueryRepository.findCareerCards(projectId);

        assertThat(cards).extracting(CareerCard::cardId).containsExactly(backendFirst, backendSecond, frontend, android);
        CareerCard first = cards.getFirst();
        assertThat(first.jobRole()).isEqualTo("BE");
        assertThat(first.template()).isEqualTo("tech_decision");
        assertThat(first.category()).isEqualTo("장애 대응");
        assertThat(first.resumeBullets()).containsExactly("문장 1", "문장 2");
        assertThat(first.techs()).containsExactly("Spring", "JPA");
        assertThat(first.options()).containsExactly("방법 A", "방법 B");
        assertThat(first.problem()).isEqualTo("문제");
        assertThat(first.decisionReason()).isNull();
        assertThat(first.limits()).isNull();
    }

    @Test
    @DisplayName("카드 근거를 source_order 순으로 읽고, 삭제된 경험과 자료, 받아들인 제보 자료는 빼며, 근거가 없는 카드는 결과에 없다")
    void findsCareerCardSources() {
        long card = fixtures.careerCard(projectId, "BE", "problem_solving", 0, false);
        long removedOnlyCard = fixtures.careerCard(projectId, "BE", "problem_solving", 1, false);
        long blog = fixtures.document("blog-post", "card-blog").project(projectId).topic(TOPIC).insert();
        long pr = fixtures.document("github-pr", "card-pr").project(projectId).topic(TOPIC).insert();
        long doc = fixtures.document("github-teamdoc", "card-doc").project(projectId).topic(TOPIC).insert();
        long deletedDocument = fixtures.document("github-pr", "card-deleted").project(projectId).deleted().insert();
        long goneExperienceDocument = fixtures.document("github-pr", "card-gone").project(projectId).insert();
        long removed = fixtures.document("github-pr", "card-removed").project(projectId).insert();
        fixtures.removalReport(projectId, "github-pr", "card-removed", "ACCEPTED");
        fixtures.cardSource(card, fixtures.experience(blog, false), 2);
        fixtures.cardSource(card, fixtures.experience(pr, false), 0);
        fixtures.cardSource(card, fixtures.experience(doc, false), 1);
        fixtures.cardSource(card, fixtures.experience(deletedDocument, false), 3);
        fixtures.cardSource(card, fixtures.experience(goneExperienceDocument, true), 4);
        fixtures.cardSource(removedOnlyCard, fixtures.experience(removed, false), 0);

        Map<Long, List<ResourceCard>> sources = projectResourceQueryRepository.findCareerCardSources(
                List.of(card, removedOnlyCard)
        );

        assertThat(sources).containsOnlyKeys(card);
        assertThat(sources.get(card)).extracting(ResourceCard::resourceId).containsExactly(pr, doc, blog);
        assertThat(sources.get(card).getFirst().team())
                .isEqualTo(new ResourceCard.Team("샤라웃", fixtures.slugOf(projectId), null));
        assertThat(projectResourceQueryRepository.findCareerCardSources(List.of())).isEmpty();
    }

    private long idOf(String externalKey) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM resource_documents WHERE external_key = ?",
                Long.class,
                externalKey
        );
    }
}
