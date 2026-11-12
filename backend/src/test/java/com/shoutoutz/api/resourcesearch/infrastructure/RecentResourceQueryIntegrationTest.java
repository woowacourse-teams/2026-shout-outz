package com.shoutoutz.api.resourcesearch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcesearch.application.ResourceCardQueryRepository;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.support.ResourceDocumentFixture;
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
class RecentResourceQueryIntegrationTest {

    private static final Instant DAY1 = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant DAY2 = Instant.parse("2026-10-02T00:00:00Z");
    private static final Instant DAY3 = Instant.parse("2026-10-03T00:00:00Z");

    @Autowired
    private ResourceCardQueryRepository resourceCardQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ResourceDocumentFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ResourceDocumentFixture(jdbcTemplate);
        jdbcTemplate.update("UPDATE resource_documents SET deleted_at = now() WHERE deleted_at IS NULL");
    }

    @Test
    @DisplayName("게시 시각 최신순으로 주고 같으면 ID가 큰 순이다")
    void ordersByPublishedAtThenIdDescending() {
        long older = blog(DAY1, "{}");
        long sameTimeFirst = blog(DAY2, "{}");
        long sameTimeSecond = blog(DAY2, "{}");

        assertThat(ids(ResourceSection.BLOG, 10)).containsExactly(sameTimeSecond, sameTimeFirst, older);
    }

    @Test
    @DisplayName("등록된 프로젝트의 블로그는 팀마다 최신 글 하나만 준다")
    void keepsLatestBlogPerRegisteredTeam() {
        long projectId = fixture.approvedProject("festabook", "페스타북");
        fixture.document("blog-post").projectId(projectId).publishedAt(DAY1).save();
        long latest = fixture.document("blog-post").projectId(projectId).publishedAt(DAY3).save();
        long other = blog(DAY2, "{}");

        assertThat(ids(ResourceSection.BLOG, 10)).containsExactly(latest, other);
    }

    @Test
    @DisplayName("제보로 삭제가 승인된 글은 빼고 같은 팀의 다음 최신 글을 준다")
    void skipsRemovedBlogAndKeepsNextLatestOfTeam() {
        long projectId = fixture.approvedProject("festabook", "페스타북");
        long older = fixture.document("blog-post").projectId(projectId).publishedAt(DAY1).save();
        long removed = fixture.document("blog-post").projectId(projectId).publishedAt(DAY3).save();
        fixture.removalReport(removed, "ACCEPTED");

        assertThat(ids(ResourceSection.BLOG, 10)).containsExactly(older);
    }

    @Test
    @DisplayName("등록 전 팀의 블로그는 수집 때 저장한 팀 이름으로 묶는다")
    void groupsUnregisteredBlogsByCollectedTeamName() {
        blog(DAY1, "{\"project_name\": \"모모\"}");
        long latest = blog(DAY2, "{\"project_name\": \"모모\"}");

        assertThat(ids(ResourceSection.BLOG, 10)).containsExactly(latest);
    }

    @Test
    @DisplayName("수집한 팀 이름이 없는 등록 전 팀의 블로그는 화면에 보이는 팀 이름처럼 저장소로 묶는다")
    void groupsUnlinkedBlogsByRepository() {
        fixture.document("blog-post").publishedAt(DAY1).repo("woowacourse-teams/2026-poudy").save();
        long latest = fixture.document("blog-post").publishedAt(DAY2).repo("woowacourse-teams/2026-poudy").save();
        long otherRepository = fixture.document("blog-post").publishedAt(DAY1).repo("woowacourse-teams/2026-bibbidi").save();

        assertThat(ids(ResourceSection.BLOG, 10)).containsExactly(latest, otherRepository);
    }

    @Test
    @DisplayName("팀을 알 수 없는 블로그끼리는 묶지 않는다")
    void doesNotGroupBlogsWithoutTeam() {
        long first = blog(DAY1, "{}");
        long second = blog(DAY2, "{\"project_name\": \"  \"}");

        assertThat(ids(ResourceSection.BLOG, 10)).containsExactly(second, first);
    }

    @Test
    @DisplayName("테코톡은 발표자마다 최신 영상 하나만 주고 발표자를 모르면 따로 둔다")
    void keepsLatestTalkPerSpeaker() {
        talk(DAY1, "{\"speaker\": \"벨로\"}");
        long latestBello = talk(DAY3, "{\"speaker\": \"벨로\"}");
        long unknownFirst = talk(DAY1, "{}");
        long unknownSecond = talk(DAY2, "{}");

        assertThat(ids(ResourceSection.TALK, 10)).containsExactly(latestBello, unknownSecond, unknownFirst);
    }

    @Test
    @DisplayName("한줄 소개나 게시 시각이 없거나 삭제된 자료는 뺀다")
    void excludesIncompleteOrDeletedDocuments() {
        long complete = blog(DAY1, "{}");
        fixture.document("blog-post").summary(null).publishedAt(DAY3).save();
        fixture.document("blog-post").summary("   ").publishedAt(DAY3).save();
        fixture.document("blog-post").publishedAt(null).save();
        fixture.document("blog-post").publishedAt(DAY3).deleted().save();

        assertThat(ids(ResourceSection.BLOG, 10)).containsExactly(complete);
    }

    @Test
    @DisplayName("요청한 종류의 원천만 준다")
    void returnsOnlyRequestedKind() {
        long blog = blog(DAY1, "{}");
        talk(DAY2, "{\"speaker\": \"벨로\"}");
        fixture.document("github-pr").publishedAt(DAY3).save();

        assertThat(ids(ResourceSection.BLOG, 10)).containsExactly(blog);
    }

    @Test
    @DisplayName("팀마다 하나씩 고른 뒤에 개수를 자른다")
    void limitsAfterGrouping() {
        long projectId = fixture.approvedProject("rilog", "Rilog.");
        long rilogLatest = fixture.document("blog-post").projectId(projectId).publishedAt(DAY3).save();
        fixture.document("blog-post").projectId(projectId).publishedAt(DAY3.minusSeconds(1)).save();
        long other = blog(DAY2, "{}");
        blog(DAY1, "{}");

        assertThat(ids(ResourceSection.BLOG, 2)).containsExactly(rilogLatest, other);
    }

    private long blog(Instant publishedAt, String extra) {
        return fixture.document("blog-post").publishedAt(publishedAt).extra(extra).save();
    }

    private long talk(Instant publishedAt, String extra) {
        return fixture.document("youtube-tekotalk").publishedAt(publishedAt).extra(extra).save();
    }

    private List<Long> ids(ResourceSection kind, int size) {
        return resourceCardQueryRepository.findRecent(kind, size).stream().map(ResourceCard::resourceId).toList();
    }
}
