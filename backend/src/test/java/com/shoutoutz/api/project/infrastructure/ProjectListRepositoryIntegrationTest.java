package com.shoutoutz.api.project.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.project.domain.KeywordCandidate;
import com.shoutoutz.api.project.domain.ProjectCursor;
import com.shoutoutz.api.project.domain.ProjectFilterCondition;
import com.shoutoutz.api.project.domain.ProjectFilterOptions;
import com.shoutoutz.api.project.domain.ProjectFilterOptions.CohortCount;
import com.shoutoutz.api.project.domain.ProjectFilterOptions.TechTagCount;
import com.shoutoutz.api.project.domain.ProjectMemberProfile;
import com.shoutoutz.api.project.domain.ProjectPage;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.ProjectSearchCondition;
import com.shoutoutz.api.project.domain.ProjectSort;
import com.shoutoutz.api.project.domain.ProjectSummary;
import com.shoutoutz.api.project.domain.ProjectTechTag;
import com.shoutoutz.api.project.domain.PublicProjectTitle;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테스트가 로컬 DB를 함께 쓰므로, 다른 테스트가 남긴 프로젝트가 섞이지 않도록
 * 테스트마다 무작위 토큰을 제목 등에 넣고 그 토큰을 검색어로 함께 조회한다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ProjectListRepositoryIntegrationTest {

    private static final Instant BASE_TIME = Instant.parse("2026-08-01T00:00:00Z");

    private final String token = "tk" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("승인되고 삭제되지 않은 프로젝트만 조회한다.")
    void findsOnlyApprovedAndNotDeletedProjects() {
        long approved = saveProject("APPROVED", 6, BASE_TIME);
        saveProject("PENDING", 6, BASE_TIME);
        saveProject("REJECTED", 6, BASE_TIME);
        long deleted = saveProject("APPROVED", 6, BASE_TIME);
        jdbcTemplate.update("UPDATE projects SET deleted_at = now() WHERE id = ?", deleted);

        ProjectPage page = findAll(condition(token));

        assertThat(ids(page)).containsExactly(approved);
        assertThat(page.totalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("기수 필터는 선택한 기수 중 하나에 해당하는 프로젝트를 조회한다.")
    void filtersByCohortsWithOr() {
        long cohort6 = saveProject("APPROVED", 6, BASE_TIME);
        long cohort7 = saveProject("APPROVED", 7, BASE_TIME.plus(1, ChronoUnit.HOURS));
        saveProject("APPROVED", 8, BASE_TIME.plus(2, ChronoUnit.HOURS));

        ProjectPage page = findAll(new ProjectSearchCondition(
                token, List.of(6, 7), List.of(), null, ProjectSort.LATEST, 50, null));

        assertThat(ids(page)).containsExactly(cohort7, cohort6);
        assertThat(page.totalCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("기술 스택 필터는 선택한 기술 스택을 모두 사용한 프로젝트만 조회한다.")
    void filtersByTechTagsWithAnd() {
        long react = saveTechTag("React");
        long spring = saveTechTag("Spring");
        long both = saveProject("APPROVED", 6, BASE_TIME);
        long reactOnly = saveProject("APPROVED", 6, BASE_TIME.plus(1, ChronoUnit.HOURS));
        saveProject("APPROVED", 6, BASE_TIME.plus(2, ChronoUnit.HOURS));
        saveProjectTag(both, react);
        saveProjectTag(both, spring);
        saveProjectTag(reactOnly, react);

        ProjectPage bothTags = findAll(new ProjectSearchCondition(
                token, List.of(), List.of(react, spring), null, ProjectSort.LATEST, 50, null));
        ProjectPage reactTag = findAll(new ProjectSearchCondition(
                token, List.of(), List.of(react), null, ProjectSort.LATEST, 50, null));

        assertThat(ids(bothTags)).containsExactly(both);
        assertThat(ids(reactTag)).containsExactly(reactOnly, both);
    }

    @Test
    @DisplayName("검색어는 프로젝트 이름, 한 줄 소개, 참여 크루 이름에서 대소문자를 무시하고 부분 일치로 찾고, 기술 스택 이름으로는 찾지 않는다.")
    void searchesByTitleTaglineAndMemberNames() {
        long byTitle = saveProject("[" + token.toUpperCase() + "] 제목", "소개", "APPROVED", 6, BASE_TIME);
        long byTagline = saveProject("제목", "소개 " + token, "APPROVED", 6, BASE_TIME);
        long byTechTag = saveProject("제목", "소개", "APPROVED", 6, BASE_TIME);
        saveProjectTag(byTechTag, saveTechTag(token + "-framework"));
        long byMember = saveProject("제목", "소개", "APPROVED", 6, BASE_TIME);
        saveProjectMember(byMember, saveCrew(token + "크루", false));
        long byWithdrawnMember = saveProject("제목", "소개", "APPROVED", 6, BASE_TIME);
        saveProjectMember(byWithdrawnMember, saveCrew(token + "탈퇴", true));
        long byArchivedGithubLogin = saveProject("제목", "소개", "APPROVED", 6, BASE_TIME);
        saveArchivedMember(byArchivedGithubLogin, null, token + "-dev", "Archived Crew");
        long byMatchedArchivedMember = saveProject("제목", "소개", "APPROVED", 6, BASE_TIME);
        saveArchivedMember(byMatchedArchivedMember, saveCrew(token + "매칭", false), "matched-dev", "Old Name");

        ProjectPage page = findAll(condition(token));

        assertThat(ids(page)).containsExactlyInAnyOrder(
                byTitle, byTagline, byMember, byArchivedGithubLogin, byMatchedArchivedMember);
        assertThat(ids(page)).doesNotContain(byTechTag, byWithdrawnMember);
    }

    @Test
    @DisplayName("검색어의 %와 _는 와일드카드가 아닌 일반 문자로 찾는다.")
    void escapesLikeWildcards() {
        long underscore = saveProject(token + "_a", "소개", "APPROVED", 6, BASE_TIME);
        saveProject(token + "Xa", "소개", "APPROVED", 6, BASE_TIME);
        long percent = saveProject(token + "%b", "소개", "APPROVED", 6, BASE_TIME);
        saveProject(token + "Yb", "소개", "APPROVED", 6, BASE_TIME);

        assertThat(ids(findAll(condition(token + "_")))).containsExactly(underscore);
        assertThat(ids(findAll(condition(token + "%")))).containsExactly(percent);
    }

    @Test
    @DisplayName("최신순은 등록 시각 내림차순이며, 등록 시각이 같으면 id 내림차순이다.")
    void sortsLatestByCreatedAtThenId() {
        long oldest = saveProject("APPROVED", 6, BASE_TIME);
        long sameTimeFirst = saveProject("APPROVED", 6, BASE_TIME.plus(1, ChronoUnit.HOURS));
        long sameTimeSecond = saveProject("APPROVED", 6, BASE_TIME.plus(1, ChronoUnit.HOURS));

        ProjectPage page = findAll(condition(token));

        assertThat(ids(page)).containsExactly(sameTimeSecond, sameTimeFirst, oldest);
    }

    @Test
    @DisplayName("인기순은 좋아요 수 내림차순이며, 좋아요 수가 같으면 최근 등록된 프로젝트가 앞에 온다.")
    void sortsPopularByLikeCountThenCreatedAt() {
        long twoLikes = saveProject("APPROVED", 6, BASE_TIME);
        long oneLikeOlder = saveProject("APPROVED", 6, BASE_TIME);
        long oneLikeNewer = saveProject("APPROVED", 6, BASE_TIME.plus(1, ChronoUnit.HOURS));
        long noLikesNewest = saveProject("APPROVED", 6, BASE_TIME.plus(2, ChronoUnit.HOURS));
        like(twoLikes, 2);
        like(oneLikeOlder, 1);
        like(oneLikeNewer, 1);

        ProjectPage page = findAll(new ProjectSearchCondition(token, List.of(), List.of(), null, ProjectSort.POPULAR, 50, null));

        assertThat(ids(page)).containsExactly(twoLikes, oneLikeNewer, oneLikeOlder, noLikesNewest);
    }

    @ParameterizedTest
    @EnumSource(ProjectSort.class)
    @DisplayName("커서로 다음 페이지를 이어서 조회하면, 정렬 값이 같은 프로젝트가 있어도 중복이나 누락 없이 전체를 조회한다.")
    void paginatesWithCursorWithoutDuplicatesOrOmissions(ProjectSort sort) {
        long first = saveProject("APPROVED", 6, BASE_TIME);
        long second = saveProject("APPROVED", 6, BASE_TIME);
        long third = saveProject("APPROVED", 6, BASE_TIME);
        saveProject("APPROVED", 6, BASE_TIME.plus(1, ChronoUnit.HOURS));
        saveProject("APPROVED", 6, BASE_TIME.plus(1, ChronoUnit.HOURS));
        like(first, 1);
        like(second, 1);
        like(third, 1);
        List<Long> expected = ids(findAll(new ProjectSearchCondition(token, List.of(), List.of(), null, sort, 50, null)));

        List<Long> collected = new ArrayList<>();
        ProjectCursor cursor = null;
        ProjectPage page;
        do {
            page = findAll(new ProjectSearchCondition(token, List.of(), List.of(), null, sort, 2, cursor));
            assertThat(page.items()).hasSizeLessThanOrEqualTo(2);
            assertThat(page.totalCount()).isEqualTo(5);
            collected.addAll(ids(page));
            assertThat(collected).hasSizeLessThanOrEqualTo(5);
            cursor = page.nextCursor(sort);
        } while (page.hasNext());

        assertThat(collected).containsExactlyElementsOf(expected);
    }

    @Test
    @DisplayName("좋아요 수와, 삭제된 댓글을 제외하고 대댓글을 포함한 댓글 수를 조회한다.")
    void countsLikesAndComments() {
        long projectId = saveProject("APPROVED", 6, BASE_TIME);
        like(projectId, 2);
        long author = saveUser("author").getId();
        jdbcTemplate.update("INSERT INTO project_reactions (project_id, user_id, reaction_type) VALUES (?, ?, 'BOOKMARK')",
                projectId, author);
        long parent = saveComment(projectId, author, null, false);
        saveComment(projectId, author, parent, false);
        saveComment(projectId, author, null, true);

        ProjectSummary summary = findAll(condition(token)).items().getFirst();

        assertThat(summary.likeCount()).isEqualTo(2);
        assertThat(summary.commentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("프로젝트 목록은 좋아요와 북마크 수와 현재 사용자의 반응 여부를 조회한다.")
    void readsReactionCountsAndViewerState() {
        long projectId = saveProject("APPROVED", 6, BASE_TIME);
        long viewerId = saveUser("viewer").getId();
        long otherId = saveUser("other").getId();
        jdbcTemplate.update(
                "INSERT INTO project_reactions (project_id, user_id, reaction_type) VALUES (?, ?, 'LIKE'), (?, ?, 'LIKE'), (?, ?, 'BOOKMARK')",
                projectId, viewerId, projectId, otherId, projectId, viewerId
        );

        ProjectSummary summary = projectRepository.findAll(condition(token), viewerId).items().getFirst();

        assertThat(summary.likeCount()).isEqualTo(2L);
        assertThat(summary.bookmarkCount()).isEqualTo(1L);
        assertThat(summary.likedByMe()).isTrue();
        assertThat(summary.bookmarkedByMe()).isTrue();

        ProjectSummary anonymous = projectRepository.findAll(condition(token)).items().getFirst();
        assertThat(anonymous.likedByMe()).isFalse();
        assertThat(anonymous.bookmarkedByMe()).isFalse();
    }

    @Test
    @DisplayName("GitHub 스타 수를 조회하고, 아직 동기화하지 않은 프로젝트는 null 로 둔다.")
    void readsStarCount() {
        long synced = saveProject("APPROVED", 6, BASE_TIME.plusSeconds(1));
        long notSynced = saveProject("APPROVED", 6, BASE_TIME);
        jdbcTemplate.update("UPDATE projects SET star_count = 128 WHERE id = ?", synced);

        List<ProjectSummary> items = findAll(condition(token)).items();

        assertThat(items).extracting(ProjectSummary::id).containsExactly(synced, notSynced);
        assertThat(items.getFirst().starCount()).isEqualTo(128);
        assertThat(items.getLast().starCount()).isNull();
    }

    @Test
    @DisplayName("카드마다 기술 스택과 팀원을 등록 순서대로 붙이고, 이관 프로젝트에는 이관 팀원을 붙인다.")
    void attachesTechTagsAndMembersToEachCard() {
        long owner = saveCrew("등록자", false);
        long teammate = saveCrew("팀원", false);
        long withdrawn = saveCrew("탈퇴자", true);
        long registered = saveProject("APPROVED", 6, BASE_TIME.plus(2, ChronoUnit.HOURS));
        jdbcTemplate.update("UPDATE projects SET registered_by = ? WHERE id = ?", owner, registered);
        long spring = saveTechTag("Spring");
        long react = saveTechTag("React");
        saveProjectTag(registered, spring, 1);
        saveProjectTag(registered, react, 0);
        saveProjectMember(registered, withdrawn, 2);
        saveProjectMember(registered, teammate, 1);
        saveProjectMember(registered, owner, 0);
        long archived = saveProject("APPROVED", 7, BASE_TIME.plus(1, ChronoUnit.HOURS));
        saveArchivedMember(archived, null, token + "-dev", "Archived Crew");
        long withoutTagsAndMembers = saveProject("APPROVED", 6, BASE_TIME);

        Map<Long, ProjectSummary> cards = findAll(condition(token)).items().stream()
                .collect(Collectors.toMap(ProjectSummary::id, Function.identity()));

        assertThat(cards.get(registered).techTags()).extracting(ProjectTechTag::id).containsExactly(react, spring);
        assertThat(cards.get(registered).members()).extracting(ProjectMemberProfile::userId)
                .containsExactly(owner, teammate, withdrawn);
        assertThat(cards.get(registered).members().getLast().displayName()).isEqualTo("탈퇴한 사용자");
        assertThat(cards.get(archived).members())
                .containsExactly(ProjectMemberProfile.archived("Archived Crew", 7, null, null));
        assertThat(cards.get(withoutTagsAndMembers).techTags()).isEmpty();
        assertThat(cards.get(withoutTagsAndMembers).members()).isEmpty();
    }

    @Test
    @DisplayName("필터 옵션의 조건에 맞는 프로젝트 수는 같은 조건의 목록 전체 개수와 같다.")
    void matchedProjectCountEqualsListTotalCount() {
        saveProject("APPROVED", 6, BASE_TIME);
        saveProject("APPROVED", 7, BASE_TIME);
        saveProject("PENDING", 6, BASE_TIME);
        long deleted = saveProject("APPROVED", 6, BASE_TIME);
        jdbcTemplate.update("UPDATE projects SET deleted_at = now() WHERE id = ?", deleted);
        saveProject("검색어가 없는 제목", "소개", "APPROVED", 6, BASE_TIME);

        ProjectFilterOptions options = findFilterOptions(List.of(6), List.of());
        ProjectPage page = findAll(new ProjectSearchCondition(token, List.of(6), List.of(), null, ProjectSort.LATEST, 50, null));

        assertThat(options.matchedProjectCount()).isEqualTo(1).isEqualTo(page.totalCount());
    }

    @Test
    @DisplayName("필터 옵션은 모든 기수를 최신 기수부터 반환하고, 프로젝트가 없는 기수는 0개로 센다.")
    void returnsAllCohortsDescendingWithZeroCounts() {
        saveProject("APPROVED", 6, BASE_TIME);

        ProjectFilterOptions options = findFilterOptions(List.of(), List.of());

        assertThat(options.cohorts()).extracting(CohortCount::cohort).containsExactlyElementsOf(Cohort.descending());
        assertThat(cohortCounts(options)).containsEntry(6, 1L).containsEntry(8, 0L);
    }

    @Test
    @DisplayName("기수별 프로젝트 수는 선택한 기수로 거르지 않고, 검색어와 기술 스택 조건만 적용해 센다.")
    void countsCohortsWithoutSelectedCohorts() {
        long spring = saveTechTag("Spring");
        long cohort6 = saveProject("APPROVED", 6, BASE_TIME);
        long cohort7 = saveProject("APPROVED", 7, BASE_TIME);
        saveProject("APPROVED", 6, BASE_TIME);
        saveProjectTag(cohort6, spring);
        saveProjectTag(cohort7, spring);

        ProjectFilterOptions options = findFilterOptions(List.of(6), List.of(spring));

        assertThat(cohortCounts(options)).containsEntry(6, 1L).containsEntry(7, 1L);
        assertThat(options.matchedProjectCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("기술 스택별 프로젝트 수는 현재 조건에 그 기술 스택을 추가로 선택했을 때의 수이고, 사용한 프로젝트가 없으면 0개다.")
    void countsTechTagsAsIfAdditionallySelected() {
        long react = saveTechTag("React");
        long spring = saveTechTag("Spring");
        long docker = saveTechTag("Docker");
        long reactAndSpring = saveProject("APPROVED", 6, BASE_TIME);
        long reactOnly = saveProject("APPROVED", 6, BASE_TIME);
        long otherCohort = saveProject("APPROVED", 7, BASE_TIME);
        saveProjectTag(reactAndSpring, react);
        saveProjectTag(reactAndSpring, spring);
        saveProjectTag(reactOnly, react);
        saveProjectTag(otherCohort, react);
        saveProjectTag(otherCohort, spring);

        ProjectFilterOptions options = findFilterOptions(List.of(6), List.of(react));

        assertThat(options.matchedProjectCount()).isEqualTo(2);
        assertThat(techTagCounts(options))
                .containsEntry(react, 2L)
                .containsEntry(spring, 1L)
                .containsEntry(docker, 0L);
    }

    @Test
    @DisplayName("비활성 기술 스택은 필터 옵션에 포함하지 않는다.")
    void excludesInactiveTechTags() {
        long inactive = saveTechTag("Inactive");
        jdbcTemplate.update("UPDATE tech_tags SET is_active = false WHERE id = ?", inactive);
        saveProjectTag(saveProject("APPROVED", 6, BASE_TIME), inactive);

        ProjectFilterOptions options = findFilterOptions(List.of(), List.of());

        assertThat(techTagCounts(options)).doesNotContainKey(inactive);
    }

    private ProjectPage findAll(ProjectSearchCondition condition) {
        return projectRepository.findAll(condition);
    }

    private ProjectFilterOptions findFilterOptions(List<Integer> cohorts, List<Long> techTagIds) {
        return projectRepository.findFilterOptions(new ProjectFilterCondition(token, cohorts, techTagIds));
    }

    private static Map<Integer, Long> cohortCounts(ProjectFilterOptions options) {
        return options.cohorts().stream()
                .collect(Collectors.toMap(count -> count.cohort().getValue(), CohortCount::projectCount));
    }

    @Test
    @DisplayName("기술 주제를 고르면 그 주제의 팀 자료가 있는 프로젝트만 조회하고, 내려간 자료, 받아들인 제보 자료, 테코톡만 있으면 뺀다.")
    void filtersByTopic() {
        long withTopic = saveProject("APPROVED", 6, BASE_TIME);
        long otherTopic = saveProject("APPROVED", 6, BASE_TIME.plus(1, ChronoUnit.HOURS));
        long onlyDeleted = saveProject("APPROVED", 6, BASE_TIME.plus(2, ChronoUnit.HOURS));
        long onlyRemoved = saveProject("APPROVED", 6, BASE_TIME.plus(3, ChronoUnit.HOURS));
        long onlyTalk = saveProject("APPROVED", 6, BASE_TIME.plus(4, ChronoUnit.HOURS));
        saveTopicResource(withTopic, "github-pr", "concurrency", false);
        saveTopicResource(otherTopic, "github-pr", "n_plus_one", false);
        saveTopicResource(onlyDeleted, "github-pr", "concurrency", true);
        String removedKey = saveTopicResource(onlyRemoved, "github-teamdoc", "concurrency", false);
        acceptRemoval(onlyRemoved, "github-teamdoc", removedKey);
        saveTopicResource(onlyTalk, "youtube-tekotalk", "concurrency", false);

        ProjectPage page = findAll(topicCondition(token, "concurrency"));

        assertThat(ids(page)).containsExactly(withTopic);
        assertThat(page.totalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("기술 주제 조건은 다른 조건과 함께 걸리고, 없는 주제 id 면 빈 목록이다.")
    void combinesTopicWithOtherFilters() {
        long cohort6 = saveProject("APPROVED", 6, BASE_TIME);
        long cohort7 = saveProject("APPROVED", 7, BASE_TIME.plus(1, ChronoUnit.HOURS));
        saveTopicResource(cohort6, "blog-post", "concurrency", false);
        saveTopicResource(cohort7, "github-pr", "concurrency", false);

        ProjectPage cohort7Only = findAll(new ProjectSearchCondition(
                token, List.of(7), List.of(), "concurrency", ProjectSort.LATEST, 50, null));
        ProjectPage bothCohorts = findAll(topicCondition(token, "concurrency"));
        ProjectPage unknownTopic = findAll(topicCondition(token, "unknown"));

        assertThat(ids(cohort7Only)).containsExactly(cohort7);
        assertThat(ids(bothCohorts)).containsExactly(cohort7, cohort6);
        assertThat(ids(unknownTopic)).isEmpty();
        assertThat(unknownTopic.totalCount()).isZero();
    }

    @Test
    @DisplayName("이름 비교에 쓸 프로젝트 이름은 승인되고 삭제되지 않은 프로젝트에서만 조회한다.")
    void findsTitlesOfPublicProjectsOnly() {
        long approved = saveProject(token + " 공개", "소개", "APPROVED", 6, BASE_TIME);
        long pending = saveProject(token + " 대기", "소개", "PENDING", 6, BASE_TIME);
        long deleted = saveProject(token + " 삭제", "소개", "APPROVED", 6, BASE_TIME);
        jdbcTemplate.update("UPDATE projects SET deleted_at = now() WHERE id = ?", deleted);

        List<PublicProjectTitle> titles = projectRepository.findPublicProjectTitles();

        assertThat(titles).contains(new PublicProjectTitle(approved, token + " 공개"));
        assertThat(titles).extracting(PublicProjectTitle::id).doesNotContain(pending, deleted);
    }

    @Test
    @DisplayName("프로젝트 id 를 정해 조회하면 그 프로젝트 중 다른 조건에 맞는 것만 조회하고 전체 개수도 같은 조건으로 센다.")
    void restrictsToGivenProjectIdsWithOtherFilters() {
        long cohort6 = saveProject("APPROVED", 6, BASE_TIME);
        long cohort7 = saveProject("APPROVED", 7, BASE_TIME.plus(1, ChronoUnit.HOURS));
        saveProject("APPROVED", 7, BASE_TIME.plus(2, ChronoUnit.HOURS));

        ProjectPage page = findAll(new ProjectSearchCondition(
                null, List.of(7), List.of(), null, ProjectSort.LATEST, 50, null, List.of(cohort6, cohort7)));

        assertThat(ids(page)).containsExactly(cohort7);
        assertThat(page.totalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("관련도순은 미리 고른 프로젝트 순서대로 조회하고, 커서로 이어 읽으면 그 순서를 이어간다.")
    void sortsRelevanceByGivenOrderAndPaginates() {
        long first = saveProject("APPROVED", 6, BASE_TIME);
        long second = saveProject("APPROVED", 6, BASE_TIME.plus(2, ChronoUnit.HOURS));
        long third = saveProject("APPROVED", 6, BASE_TIME.plus(1, ChronoUnit.HOURS));
        List<Long> rankedIds = List.of(second, first, third);

        ProjectPage firstPage = findAll(new ProjectSearchCondition(
                null, List.of(), List.of(), null, ProjectSort.RELEVANCE, 2, null, rankedIds));
        ProjectCursor cursor = firstPage.nextCursor(ProjectSort.RELEVANCE);
        ProjectPage secondPage = findAll(new ProjectSearchCondition(
                null, List.of(), List.of(), null, ProjectSort.RELEVANCE, 2, cursor, rankedIds));

        assertThat(ids(firstPage)).containsExactly(second, first);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(ids(secondPage)).containsExactly(third);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.totalCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("고른 프로젝트 없이 관련도순으로 조회하면 최신순과 같다.")
    void sortsRelevanceLikeLatestWithoutGivenOrder() {
        long older = saveProject("APPROVED", 6, BASE_TIME);
        long newer = saveProject("APPROVED", 6, BASE_TIME.plus(1, ChronoUnit.HOURS));

        ProjectPage page = findAll(new ProjectSearchCondition(
                token, List.of(), List.of(), null, ProjectSort.RELEVANCE, 50, null));

        assertThat(ids(page)).containsExactly(newer, older);
    }

    @Test
    @DisplayName("직접 검색 후보는 필터를 통과한 프로젝트 중 이름, 소개, 크루 이름, 저장소와 배포 주소에 검색어가 들었거나 자료 검색에 걸린 프로젝트다.")
    void findsKeywordCandidates() {
        long byTitle = saveProject("[" + token + "] 제목", "소개", "APPROVED", 6, BASE_TIME);
        long byTagline = saveProject("제목", "소개 " + token, "APPROVED", 6, BASE_TIME);
        long byRepository = saveProject("제목", "소개", "APPROVED", 6, BASE_TIME);
        jdbcTemplate.update("UPDATE projects SET github_repository_url = ? WHERE id = ?",
                "https://github.com/woowacourse-teams/2025-" + token, byRepository);
        long byDeployment = saveProject("제목", "소개", "APPROVED", 6, BASE_TIME);
        jdbcTemplate.update("UPDATE projects SET deployment_url = ? WHERE id = ?",
                "https://" + token + ".example.com", byDeployment);
        long byResource = saveProject("제목", "소개", "APPROVED", 6, BASE_TIME);
        long unrelated = saveProject("제목", "소개", "APPROVED", 6, BASE_TIME);
        long otherCohort = saveProject("[" + token + "] 7기", "소개", "APPROVED", 7, BASE_TIME);
        long pendingResource = saveProject("제목", "소개", "PENDING", 6, BASE_TIME);

        List<KeywordCandidate> candidates = projectRepository.findKeywordCandidates(
                new ProjectFilterCondition(token, List.of(6), List.of()),
                List.of(byResource, pendingResource, otherCohort)
        );

        assertThat(candidates).containsExactlyInAnyOrder(
                new KeywordCandidate(byTitle, true),
                new KeywordCandidate(byTagline, false),
                new KeywordCandidate(byRepository, false),
                new KeywordCandidate(byDeployment, false),
                new KeywordCandidate(byResource, false)
        );
        assertThat(candidates).extracting(KeywordCandidate::id).doesNotContain(unrelated, otherCohort, pendingResource);
    }

    private static Map<Long, Long> techTagCounts(ProjectFilterOptions options) {
        return options.techTags().stream()
                .collect(Collectors.toMap(TechTagCount::id, TechTagCount::projectCount));
    }

    private static ProjectSearchCondition condition(String keyword) {
        return new ProjectSearchCondition(keyword, List.of(), List.of(), null, ProjectSort.LATEST, 50, null);
    }

    private static ProjectSearchCondition topicCondition(String keyword, String topicId) {
        return new ProjectSearchCondition(keyword, List.of(), List.of(), topicId, ProjectSort.LATEST, 50, null);
    }

    private static List<Long> ids(ProjectPage page) {
        return page.items().stream().map(ProjectSummary::id).toList();
    }

    /**
     * 프로젝트의 기술 주제 자료를 넣고 external_key 를 돌려준다.
     */
    private String saveTopicResource(long projectId, String source, String topic, boolean deleted) {
        String externalKey = "topic-" + UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO resource_documents (source, external_key, version, title, url, project_id, topic,
                                                prompt_version, updated_at, deleted_at)
                VALUES (?, ?, 1, '제목', 'https://example.com', ?, ?, 'v1', now(), CASE WHEN ? THEN now() END)
                """, source, externalKey, projectId, topic, deleted);
        return externalKey;
    }

    private void acceptRemoval(long projectId, String source, String externalKey) {
        jdbcTemplate.update("""
                INSERT INTO resource_reports (project_id, user_id, kind, resource_source, resource_external_key, status,
                                              created_at, updated_at)
                VALUES (?, ?, 'REMOVE', ?, ?, 'ACCEPTED', now(), now())
                """, projectId, saveUser("reporter").getId(), source, externalKey);
    }

    private long saveProject(String approvalStatus, int cohort, Instant createdAt) {
        return saveProject(token + " 프로젝트", "한 줄 소개", approvalStatus, cohort, createdAt);
    }

    private long saveProject(String title, String tagline, String approvalStatus, int cohort, Instant createdAt) {
        String suffix = UUID.randomUUID().toString();
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO projects (
                            cohort, team_name, slug, title, tagline, github_repository_url,
                            service_status, approval_status, created_at
                        )
                        VALUES (?, '팀', ?, ?, ?, ?, 'OPERATING', ?, ?)
                        RETURNING id
                        """,
                Long.class,
                cohort,
                "list-" + suffix,
                title,
                tagline,
                "https://github.com/woowacourse-teams/list-" + suffix,
                approvalStatus,
                Timestamp.from(createdAt)
        );
    }

    private User saveUser(String prefix) {
        return userRepository.save(User.initialize("@" + prefix + "-" + UUID.randomUUID().toString().substring(0, 8)));
    }

    private long saveCrew(String displayName, boolean withdrawn) {
        long userId = saveUser("crew").getId();
        jdbcTemplate.update(
                "INSERT INTO user_profiles (user_id, display_name, user_type, cohort, track) "
                        + "VALUES (?, ?, 'WOOWACOURSE_CREW', 6, 'BACKEND')",
                userId,
                displayName
        );
        if (withdrawn) {
            jdbcTemplate.update("UPDATE users SET status = 'DELETED', deleted_at = now() WHERE id = ?", userId);
        }
        return userId;
    }

    private long saveTechTag(String displayName) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return jdbcTemplate.queryForObject(
                "INSERT INTO tech_tags (slug, display_name) VALUES (?, ?) RETURNING id",
                Long.class,
                "list-" + suffix,
                displayName + "-" + suffix
        );
    }

    private void saveProjectTag(long projectId, long techTagId) {
        jdbcTemplate.update("INSERT INTO project_tags (project_id, tech_tag_id) VALUES (?, ?)", projectId, techTagId);
    }

    private void saveProjectMember(long projectId, long userId) {
        jdbcTemplate.update("INSERT INTO project_members (project_id, user_id) VALUES (?, ?)", projectId, userId);
    }

    private void saveProjectTag(long projectId, long techTagId, int displayOrder) {
        jdbcTemplate.update("INSERT INTO project_tags (project_id, tech_tag_id, display_order) VALUES (?, ?, ?)",
                projectId, techTagId, displayOrder);
    }

    private void saveProjectMember(long projectId, long userId, int displayOrder) {
        jdbcTemplate.update("INSERT INTO project_members (project_id, user_id, display_order) VALUES (?, ?, ?)",
                projectId, userId, displayOrder);
    }

    private void saveArchivedMember(long projectId, Long matchedUserId, String githubLogin, String displayName) {
        jdbcTemplate.update(
                """
                        INSERT INTO woowa_archived_project_members (
                            project_id, matched_user_id, github_account_id, github_login, display_name
                        )
                        VALUES (?, ?, ?, ?, ?)
                        """,
                projectId,
                matchedUserId,
                String.valueOf(Math.abs(githubLogin.hashCode())),
                githubLogin,
                displayName
        );
    }

    private void like(long projectId, int count) {
        for (int i = 0; i < count; i++) {
            jdbcTemplate.update(
                    "INSERT INTO project_reactions (project_id, user_id, reaction_type) VALUES (?, ?, 'LIKE')",
                    projectId,
                    saveUser("liker").getId()
            );
        }
    }

    private long saveComment(long projectId, long authorId, Long parentId, boolean deleted) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO project_comments (project_id, author_id, parent_id, content, deleted_at)
                        VALUES (?, ?, ?, '댓글', CASE WHEN ? THEN now() END)
                        RETURNING id
                        """,
                Long.class,
                projectId,
                authorId,
                parentId,
                deleted
        );
    }
}
