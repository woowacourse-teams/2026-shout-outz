package com.shoutoutz.api.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.shoutoutz.api.auth.application.port.GitHubOAuthIdentityPort;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 세션, CSRF 필터부터 DB 저장까지 실제 흐름으로 프로젝트 등록을 검증한다.
 * 로컬 DB 를 반복 사용해도 slug 가 겹치지 않도록 리포지토리 이름에 무작위 접미사를 붙인다.
 * 기술 태그는 시드 마이그레이션(V20260909104000__seed_tech_tags.sql)의 데이터를 사용한다.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProjectAcceptanceTest {

    private static final String PROJECTS_PATH = "/api/v1/projects";

    @LocalServerPort
    private int port;

    @MockitoBean
    private GitHubOAuthIdentityPort gitHubOAuthIdentityPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("로그인한 사용자가 프로젝트를 등록하면 프로젝트, 기술 태그, 등록자 팀원이 함께 저장된다.")
    void registersProject() {
        LoginSession author = signup("WOOWACOURSE_CREW");
        LoginSession teammate = signup("WOOWACOURSE_CREW");
        List<Long> techTagIds = techTagIds("spring-boot", "java", "react");
        String repositoryName = uniqueRepositoryName();

        Response response = registerProject(author, repositoryName, techTagIds, List.of(teammate.handle()));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.jsonPath().getString("status")).isEqualTo("success");
        long projectId = response.jsonPath().getLong("data.projectId");
        assertThat(response.jsonPath().getString("data.slug")).isEqualTo(repositoryName.substring("2026-".length()));

        Map<String, Object> project = jdbcTemplate.queryForMap(
                "SELECT registered_by, cohort, service_status, approval_status FROM projects WHERE id = ?", projectId);
        assertThat(((Number) project.get("registered_by")).longValue()).isEqualTo(author.userId());
        assertThat(((Number) project.get("cohort")).intValue()).isEqualTo(6);
        assertThat(project.get("service_status")).isEqualTo("OPERATING");
        assertThat(project.get("approval_status")).isEqualTo("PENDING");

        List<Long> savedTagIds = jdbcTemplate.queryForList(
                "SELECT tech_tag_id FROM project_tags WHERE project_id = ? ORDER BY display_order", Long.class, projectId);
        assertThat(savedTagIds).containsExactlyElementsOf(techTagIds);

        Long firstMemberId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM project_members WHERE project_id = ? AND display_order = 0", Long.class, projectId);
        assertThat(firstMemberId).isEqualTo(author.userId());
    }

    @Test
    @DisplayName("이미 등록된 리포지토리를 다시 등록하면 409를 반환한다.")
    void rejectsDuplicateRepository() {
        LoginSession author = signup("WOOWACOURSE_CREW");
        List<Long> techTagIds = techTagIds("java");
        String repositoryName = uniqueRepositoryName();
        assertThat(registerProject(author, repositoryName, techTagIds, List.of("teammate")).statusCode()).isEqualTo(201);

        Response duplicated = registerProject(author, repositoryName, techTagIds, List.of("teammate"));

        assertThat(duplicated.statusCode()).isEqualTo(409);
        assertThat(duplicated.jsonPath().getString("code")).isEqualTo("PROJECT_DUPLICATE_SLUG");
    }

    @Test
    @DisplayName("로그인한 사용자가 CSRF 토큰 없이 프로젝트 등록을 요청하면 403을 반환하고, 프로젝트를 저장하지 않는다.")
    void rejectsRegistrationWithoutCsrfToken() {
        LoginSession author = signup("WOOWACOURSE_CREW");
        String repositoryName = uniqueRepositoryName();

        Response response = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", author.sessionId())
                .contentType("application/json")
                .body(requestBody(repositoryName, techTagIds("java"), List.of("teammate")))
                .when()
                .post(PROJECTS_PATH);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.jsonPath().getString("code")).isEqualTo("CSRF_TOKEN_INVALID");
        Integer saved = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM projects WHERE slug = ?", Integer.class,
                repositoryName.substring("2026-".length()));
        assertThat(saved).isZero();
    }

    @Test
    @DisplayName("로그인하지 않고 프로젝트 등록을 요청하면 401을 반환한다.")
    void rejectsAnonymousRegistration() {
        Response response = RestAssured.given()
                .port(port)
                .contentType("application/json")
                .body(requestBody(uniqueRepositoryName(), techTagIds("java"), List.of("teammate")))
                .when()
                .post(PROJECTS_PATH);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.jsonPath().getString("code")).isEqualTo("UNAUTHORIZED");
    }

    private Response registerProject(
            LoginSession author,
            String repositoryName,
            List<Long> techTagIds,
            List<String> memberHandles
    ) {
        return RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", author.sessionId())
                .header("X-CSRF-Token", author.csrfToken())
                .contentType("application/json")
                .body(requestBody(repositoryName, techTagIds, memberHandles))
                .when()
                .post(PROJECTS_PATH);
    }

    private static Map<String, Object> requestBody(
            String repositoryName,
            List<Long> techTagIds,
            List<String> memberHandles
    ) {
        return Map.of(
                "title", "루프 (Loop)",
                "teamName", "루프팀",
                "tagline", "스프린트 회고와 액션 아이템을 하나로 엮은 실시간 협업 도구",
                "cohort", 6,
                "githubRepositoryUrl", "https://github.com/woowacourse-teams/" + repositoryName,
                "deploymentUrl", "https://loop.team",
                "techTagIds", techTagIds,
                "memberHandles", memberHandles
        );
    }

    private List<Long> techTagIds(String... slugs) {
        return java.util.Arrays.stream(slugs)
                .map(slug -> jdbcTemplate.queryForObject("SELECT id FROM tech_tags WHERE slug = ?", Long.class, slug))
                .toList();
    }

    private static String uniqueRepositoryName() {
        return "2026-loop-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * GitHub 신원 조회만 목으로 바꾸고, 실제 로그인, 가입 API 를 거쳐 인증 세션과 CSRF 토큰을 얻는다.
     */
    private LoginSession signup(String userType) {
        Response authorization = RestAssured.given()
                .port(port)
                .redirects().follow(false)
                .when()
                .get("/oauth2/authorization/github");
        String state = UriComponentsBuilder.fromUriString(authorization.header("Location"))
                .build().getQueryParams().getFirst("state");
        given(gitHubOAuthIdentityPort.fetchIdentity(eq("authorization-code"), anyString()))
                .willReturn(new OAuthIdentity(
                        OAuthProvider.GITHUB,
                        Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits()),
                        "https://avatars.githubusercontent.com/u/12345678",
                        "https://github.com/dhyepark"
                ));
        Response callback = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", authorization.cookie("JSESSIONID"))
                .queryParam("code", "authorization-code")
                .queryParam("state", state)
                .redirects().follow(false)
                .when()
                .get("/login/oauth2/code/github");
        String pendingSessionId = callback.cookie("JSESSIONID");
        String csrfToken = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", pendingSessionId)
                .when()
                .get("/api/v1/auth/session")
                .jsonPath().getString("data.csrfToken");
        String handle = "crew-" + UUID.randomUUID().toString().substring(0, 8);

        Response signup = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", pendingSessionId)
                .header("X-CSRF-Token", csrfToken)
                .contentType("application/json")
                .body(Map.of(
                        "handle", handle,
                        "displayName", "크루",
                        "userType", userType,
                        "track", "BACKEND",
                        "cohort", 6
                ))
                .when()
                .post("/api/v1/auth/signup");

        assertThat(signup.statusCode()).as(signup.asString()).isEqualTo(201);
        return new LoginSession(
                signup.cookie("JSESSIONID"),
                csrfToken,
                signup.jsonPath().getLong("data.userId"),
                handle
        );
    }

    private record LoginSession(String sessionId, String csrfToken, long userId, String handle) {
    }
}
