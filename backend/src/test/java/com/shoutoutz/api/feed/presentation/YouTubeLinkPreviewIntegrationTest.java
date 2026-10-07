package com.shoutoutz.api.feed.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shoutoutz.api.auth.presentation.security.CsrfTokenManager;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.user.domain.account.UserRole;
import io.restassured.path.json.JsonPath;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** 실제 YouTube와 로컬 PostgreSQL을 사용하는 수동 실행용 통합 테스트. */
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "link-preview.enabled=true",
        "aws.s3.region=ap-northeast-2"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RUN_YOUTUBE_INTEGRATION_TEST", matches = "true")
class YouTubeLinkPreviewIntegrationTest {

    private static final String VIDEO_URL =
            "https://www.youtube.com/watch?v=KifSefVEdBs&list=RDKifSefVEdBs&start_radio=1";
    private static final String EXPECTED_VIDEO_TITLE =
            "스파이더맨 브랜드 뉴 데이 OST - 엔딩곡으로 정말 너무나도 적절했던 노래..., "
                    + "Steve Lacy - oh yeah? [가사/해석/lyrics/edit]";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AuthSessionAccessor sessionAccessor;
    @Autowired private CsrfTokenManager csrfTokenManager;

    private Long userId;
    private Long categoryId;
    private String testUrl;

    @Test
    @DisplayName("로그인 세션으로 유튜브 피드를 등록하면 실제 OG 수집 후 조회에 영상 정보가 반환된다")
    void createsFeedAndReturnsCollectedYouTubePreview() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (handle) VALUES (?) RETURNING id", Long.class, "@og-test-" + suffix);
        jdbcTemplate.update("INSERT INTO user_profiles (user_id, display_name) VALUES (?, ?)",
                userId, "OG 통합 테스트");
        categoryId = jdbcTemplate.queryForObject(
                "INSERT INTO categories (slug, display_name, is_active) VALUES (?, ?, true) RETURNING id",
                Long.class, "og-test-" + suffix, "OG 테스트 " + suffix);
        jdbcTemplate.update("INSERT INTO category_feed_types (category_id, feed_type) VALUES (?, 'POST')",
                categoryId);

        // fragment는 HTTP 요청에 전송되지 않는다. 캐시 키만 분리해 기존 READY 캐시로 통과하지 않게 한다.
        testUrl = VIDEO_URL + "#integration-" + suffix;
        MockHttpSession session = new MockHttpSession();
        sessionAccessor.saveAuthentication(session, userId, UserRole.USER);
        String csrfToken = csrfTokenManager.getOrCreate(session);
        mockMvc.perform(get("/api/v1/auth/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.data.userId").value(userId));

        String created = mockMvc.perform(post("/api/v1/feeds")
                        .session(session)
                        .header("X-CSRF-Token", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"유튜브 OG 통합 테스트", "content":"%s",
                                 "categoryIds":[%d], "mediaIds":[], "feedType":"POST"}
                                """.formatted(testUrl, categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.author.userId").value(userId))
                .andReturn().getResponse().getContentAsString();
        long feedId = JsonPath.from(created).getLong("data.feedId");

        // 테스트 전체를 @Transactional로 감싸면 AFTER_COMMIT 수집이 실행되지 않는다.
        Map<String, Object> cache = awaitCollection();
        assertThat(cache.get("status")).as("YouTube 수집 결과: %s", cache).isEqualTo("READY");
        assertThat(cache.get("title")).as("DB에 저장한 제목이 영상 제목과 일치해야 한다")
                .isEqualTo(EXPECTED_VIDEO_TITLE);

        String response = mockMvc.perform(get("/api/v1/feeds/{feedId}", feedId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedId").value(feedId))
                .andExpect(jsonPath("$.data.linkPreview.url").value(testUrl))
                .andReturn().getResponse().getContentAsString();
        JsonPath json = JsonPath.from(response);
        assertThat(json.getString("data.linkPreview.title"))
                .as("피드 조회의 미리보기 제목이 영상 제목과 일치해야 한다")
                .isEqualTo(EXPECTED_VIDEO_TITLE);
        assertThat(json.getString("data.linkPreview.description")).isNull();
        assertThat(json.getString("data.linkPreview.imageUrl"))
                .isEqualTo("https://i.ytimg.com/vi/KifSefVEdBs/hqdefault.jpg");
        assertThat(json.getString("data.linkPreview.siteName")).isEqualTo("YouTube");
        System.out.printf("YouTube integration feedId=%d, userId=%d%nGET response=%s%n",
                feedId, userId, response);
    }

    private Map<String, Object> awaitCollection() throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        Map<String, Object> cache;
        do {
            cache = jdbcTemplate.queryForMap(
                    "SELECT status, title, image_url, attempts FROM link_preview_cache WHERE url = ?", testUrl);
            if ("READY".equals(cache.get("status")) || "FAILED".equals(cache.get("status"))) {
                return cache;
            }
            Thread.sleep(200);
        } while (System.nanoTime() < deadline);
        return cache;
    }

    @AfterEach
    void cleanUpOwnFixtures() {
        // 로컬에서 직접 조회하려면 보존 옵션을 켠다. 기존 사용자/피드/캐시는 건드리지 않는다.
        if (Boolean.parseBoolean(System.getenv("KEEP_YOUTUBE_TEST_DATA"))) {
            return;
        }
        if (userId != null) {
            jdbcTemplate.update("DELETE FROM feeds WHERE author_id = ?", userId);
        }
        if (testUrl != null) {
            jdbcTemplate.update("DELETE FROM link_preview_cache WHERE url = ?", testUrl);
        }
        if (categoryId != null) {
            jdbcTemplate.update("DELETE FROM categories WHERE id = ?", categoryId);
        }
        if (userId != null) {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        }
    }
}
