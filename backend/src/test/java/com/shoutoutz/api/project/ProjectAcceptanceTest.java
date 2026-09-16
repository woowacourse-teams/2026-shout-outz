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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
 * 기술 태그는 시드 마이그레이션(V20260913120000__seed_tech_tags.sql)의 데이터를 사용한다.
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
    @DisplayName("로그인한 사용자가 프로젝트를 등록하면 프로젝트, 기술 태그, 등록자부터 이어지는 팀원이 함께 저장된다.")
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

        List<Long> memberIds = jdbcTemplate.queryForList(
                "SELECT user_id FROM project_members WHERE project_id = ? ORDER BY display_order", Long.class, projectId);
        assertThat(memberIds).containsExactly(author.userId(), teammate.userId());
    }

    @Test
    @DisplayName("표기만 다를 뿐 이미 등록된 리포지토리를 다시 등록하면 409를 반환한다.")
    void rejectsDuplicateRepository() {
        LoginSession author = signup("WOOWACOURSE_CREW");
        LoginSession teammate = signup("WOOWACOURSE_CREW");
        List<Long> techTagIds = techTagIds("java");
        String repositoryName = uniqueRepositoryName();
        List<String> memberHandles = List.of(teammate.handle());
        assertThat(registerProject(author, repositoryName, techTagIds, memberHandles).statusCode()).isEqualTo(201);

        Response duplicated = registerProject(author, requestBodyWithUrl(
                "https://www.github.com/Woowacourse-Teams/" + repositoryName.toUpperCase(Locale.ROOT) + ".git/",
                techTagIds,
                memberHandles
        ));

        assertThat(duplicated.statusCode()).isEqualTo(409);
        assertThat(duplicated.jsonPath().getString("code")).isEqualTo("PROJECT_DUPLICATE_GITHUB_REPOSITORY");
    }

    @Test
    @DisplayName("다른 리포지토리라도 이름이 같아 주소가 겹치면 409를 반환한다.")
    void rejectsDuplicateSlug() {
        LoginSession author = signup("WOOWACOURSE_CREW");
        LoginSession teammate = signup("WOOWACOURSE_CREW");
        List<Long> techTagIds = techTagIds("java");
        String repositoryName = uniqueRepositoryName();
        List<String> memberHandles = List.of(teammate.handle());
        assertThat(registerProject(author, repositoryName, techTagIds, memberHandles).statusCode()).isEqualTo(201);

        Response duplicated = registerProject(author, requestBodyWithUrl(
                "https://github.com/another-owner/" + repositoryName,
                techTagIds,
                memberHandles
        ));

        assertThat(duplicated.statusCode()).isEqualTo(409);
        assertThat(duplicated.jsonPath().getString("code")).isEqualTo("PROJECT_DUPLICATE_SLUG");
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 팀원으로 넣으면 400을 반환하고, 프로젝트를 저장하지 않는다.")
    void rejectsUnknownMember() {
        LoginSession author = signup("WOOWACOURSE_CREW");
        String repositoryName = uniqueRepositoryName();

        Response response = registerProject(author, repositoryName, techTagIds("java"), List.of("no-such-user"));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("code")).isEqualTo("PROJECT_INVALID_MEMBER");
        Integer saved = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM projects WHERE slug = ?", Integer.class,
                repositoryName.substring("2026-".length()));
        assertThat(saved).isZero();
    }

    @Test
    @DisplayName("본문이 존재하지 않는 이미지를 참조하면 400을 반환하고, 프로젝트를 저장하지 않는다.")
    void rejectsUnknownDescriptionMedia() {
        LoginSession author = signup("WOOWACOURSE_CREW");
        String repositoryName = uniqueRepositoryName();
        Map<String, Object> body = new HashMap<>(requestBody(repositoryName, techTagIds("java"), List.of("teammate")));
        body.put("descriptionMd", "## 화면\n![목록](media://999999999)");

        Response response = registerProject(author, body);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("code")).isEqualTo("PROJECT_INVALID_DESCRIPTION_MEDIA");
        Integer saved = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM projects WHERE slug = ?", Integer.class,
                repositoryName.substring("2026-".length()));
        assertThat(saved).isZero();
    }

    @Test
    @DisplayName("크루나 코치가 아닌 사용자가 프로젝트를 등록하면 403을 반환하고, 프로젝트를 저장하지 않는다.")
    void rejectsGeneralUserRegistration() {
        LoginSession author = signup("GENERAL");
        String repositoryName = uniqueRepositoryName();

        Response response = registerProject(author, repositoryName, techTagIds("java"), List.of("teammate"));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.jsonPath().getString("code")).isEqualTo("PROJECT_REGISTRATION_FORBIDDEN");
        Integer saved = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM projects WHERE slug = ?", Integer.class,
                repositoryName.substring("2026-".length()));
        assertThat(saved).isZero();
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

    @Test
    @DisplayName("등록자는 승인 대기 중인 본인 프로젝트를 상세 조회할 수 있고, 기술 스택과 등록자부터 이어지는 팀원이 등록 순서대로 조회된다.")
    void findsOwnPendingProjectDetail() {
        LoginSession author = signup("WOOWACOURSE_CREW");
        LoginSession teammate = signup("WOOWACOURSE_CREW");
        List<Long> techTagIds = techTagIds("react", "java");
        long projectId = registerPendingProject(author, teammate, techTagIds);

        Response response = findDetail(author, projectId);

        assertThat(response.statusCode()).as(response.asString()).isEqualTo(200);
        assertThat(response.jsonPath().getString("status")).isEqualTo("success");
        assertThat(response.jsonPath().getLong("data.id")).isEqualTo(projectId);
        assertThat(response.jsonPath().getString("data.approvalStatus")).isEqualTo("PENDING");
        assertThat(response.jsonPath().getLong("data.registeredBy")).isEqualTo(author.userId());
        assertThat(response.jsonPath().getBoolean("data.likedByMe")).isFalse();
        assertThat(response.jsonPath().getList("data.techTags.id", Long.class)).containsExactlyElementsOf(techTagIds);
        assertThat(response.jsonPath().getList("data.members.userId", Long.class))
                .containsExactly(author.userId(), teammate.userId());
        assertThat(response.jsonPath().getList("data.members.handle", String.class))
                .containsExactly(author.handle(), teammate.handle());
    }

    @Test
    @DisplayName("승인 대기 중인 프로젝트를 등록자가 아닌 사용자나 비로그인 사용자가 조회하면, 존재 여부를 숨기고 404를 반환한다.")
    void hidesPendingProjectFromOthers() {
        LoginSession author = signup("WOOWACOURSE_CREW");
        LoginSession teammate = signup("WOOWACOURSE_CREW");
        LoginSession outsider = signup("GENERAL");
        long projectId = registerPendingProject(author, teammate, techTagIds("java"));

        Response outsiderResponse = findDetail(outsider, projectId);
        Response anonymousResponse = findDetail(null, projectId);

        assertThat(outsiderResponse.statusCode()).isEqualTo(404);
        assertThat(outsiderResponse.jsonPath().getString("code")).isEqualTo("PROJECT_NOT_FOUND");
        assertThat(anonymousResponse.statusCode()).isEqualTo(404);
        assertThat(anonymousResponse.jsonPath().getString("code")).isEqualTo("PROJECT_NOT_FOUND");
    }

    @Test
    @DisplayName("승인된 프로젝트는 비로그인 사용자도 상세 조회할 수 있다.")
    void findsApprovedProjectDetailAnonymously() {
        LoginSession author = signup("WOOWACOURSE_CREW");
        LoginSession teammate = signup("WOOWACOURSE_CREW");
        long projectId = registerPendingProject(author, teammate, techTagIds("java"));
        approve(projectId);

        Response response = findDetail(null, projectId);

        assertThat(response.statusCode()).as(response.asString()).isEqualTo(200);
        assertThat(response.jsonPath().getString("data.approvalStatus")).isEqualTo("APPROVED");
        assertThat(response.jsonPath().getBoolean("data.likedByMe")).isFalse();
        assertThat(response.jsonPath().getBoolean("data.bookmarkedByMe")).isFalse();
    }

    @Test
    @DisplayName("비로그인 사용자가 승인된 프로젝트 목록을 검색어, 기술 스택, 기수로 걸러 조회하면 카드와 전체 개수를 반환한다.")
    void findsApprovedProjectsWithSearchAndFilters() {
        LoginSession author = signup("WOOWACOURSE_CREW");
        LoginSession teammate = signup("WOOWACOURSE_CREW");
        String token = uniqueToken();
        List<Long> reactAndJava = techTagIds("react", "java");
        long reactJavaProject = registerProject(author, teammate, token + " 리액트 자바", 6, reactAndJava);
        long javaProject = registerProject(author, teammate, token + " 자바", 7, techTagIds("java"));
        registerProject(author, teammate, token + " 승인 대기", 6, techTagIds("java"));
        approve(reactJavaProject);
        approve(javaProject);

        Response all = findAll(Map.of("keyword", token));
        Response byTechTags = findAll(Map.of("keyword", token, "techTagIds", joinIds(reactAndJava)));
        Response byCohort = findAll(Map.of("keyword", token, "cohorts", "7"));

        assertThat(all.statusCode()).as(all.asString()).isEqualTo(200);
        assertThat(all.jsonPath().getList("data.id", Long.class)).containsExactly(javaProject, reactJavaProject);
        assertThat(all.jsonPath().getLong("meta.totalCount")).isEqualTo(2);
        assertThat(all.jsonPath().getBoolean("meta.hasNext")).isFalse();
        assertThat(all.jsonPath().getString("meta.nextCursor")).isNull();
        assertThat(all.jsonPath().getList("data[1].techTags.id", Long.class)).containsExactlyElementsOf(reactAndJava);
        assertThat(all.jsonPath().getList("data[1].members.userId", Long.class))
                .containsExactly(author.userId(), teammate.userId());
        assertThat(byTechTags.jsonPath().getList("data.id", Long.class)).containsExactly(reactJavaProject);
        assertThat(byCohort.jsonPath().getList("data.id", Long.class)).containsExactly(javaProject);
    }

    @Test
    @DisplayName("커서로 다음 페이지를 이어서 조회하면 중복 없이 끝까지 조회하고, 마지막 페이지에는 다음 커서가 없다.")
    void paginatesProjectsWithCursor() {
        LoginSession author = signup("WOOWACOURSE_CREW");
        LoginSession teammate = signup("WOOWACOURSE_CREW");
        String token = uniqueToken();
        long oldest = registerProject(author, teammate, token + " 첫 번째", 6, techTagIds("java"));
        long middle = registerProject(author, teammate, token + " 두 번째", 6, techTagIds("java"));
        long newest = registerProject(author, teammate, token + " 세 번째", 6, techTagIds("java"));
        List.of(oldest, middle, newest).forEach(this::approve);

        Response firstPage = findAll(Map.of("keyword", token, "size", 2));
        Response lastPage = findAll(Map.of(
                "keyword", token, "size", 2, "cursor", firstPage.jsonPath().getString("meta.nextCursor")));

        assertThat(firstPage.jsonPath().getList("data.id", Long.class)).containsExactly(newest, middle);
        assertThat(firstPage.jsonPath().getBoolean("meta.hasNext")).isTrue();
        assertThat(firstPage.jsonPath().getLong("meta.totalCount")).isEqualTo(3);
        assertThat(lastPage.statusCode()).as(lastPage.asString()).isEqualTo(200);
        assertThat(lastPage.jsonPath().getList("data.id", Long.class)).containsExactly(oldest);
        assertThat(lastPage.jsonPath().getBoolean("meta.hasNext")).isFalse();
        assertThat(lastPage.jsonPath().getString("meta.nextCursor")).isNull();
        assertThat(lastPage.jsonPath().getLong("meta.totalCount")).isEqualTo(3);
    }

    @Test
    @DisplayName("목록 조회 커서가 깨졌거나 조회 개수가 범위를 벗어나면 400을 반환한다.")
    void rejectsInvalidProjectListRequest() {
        Response brokenCursor = findAll(Map.of("cursor", "broken-cursor"));
        Response tooLargeSize = findAll(Map.of("size", 51));

        assertThat(brokenCursor.statusCode()).isEqualTo(400);
        assertThat(brokenCursor.jsonPath().getString("code")).isEqualTo("PROJECT_INVALID_CURSOR");
        assertThat(tooLargeSize.statusCode()).isEqualTo(400);
        assertThat(tooLargeSize.jsonPath().getString("code")).isEqualTo("VALIDATION_FAILED");
        assertThat(tooLargeSize.jsonPath().getString("details[0].field")).isEqualTo("size");
    }

    @Test
    @DisplayName("비로그인 사용자도 handle로 사용자가 참여한 승인 프로젝트를 조회할 수 있다.")
    void findsApprovedProjectsByUserAnonymously() {
        LoginSession author = signup("WOOWACOURSE_CREW");
        LoginSession teammate = signup("WOOWACOURSE_CREW");
        long approved = registerProject(author, teammate, "사용자 프로젝트", 6, techTagIds("java"));
        registerProject(author, teammate, "승인 대기 프로젝트", 6, techTagIds("java"));
        approve(approved);

        Response response = findUserProjects(teammate.handle());

        assertThat(response.statusCode()).as(response.asString()).isEqualTo(200);
        assertThat(response.jsonPath().getList("data.id", Long.class)).containsExactly(approved);
        assertThat(response.jsonPath().getBoolean("meta.hasNext")).isFalse();
        assertThat(response.jsonPath().getString("meta.nextCursor")).isNull();
    }

    @Test
    @DisplayName("비로그인 사용자가 필터 옵션을 조회하면, 목록 조회와 같은 조건으로 센 선택지별 프로젝트 수를 반환한다.")
    void findsFilterOptionsWithSameConditionAsList() {
        LoginSession author = signup("WOOWACOURSE_CREW");
        LoginSession teammate = signup("WOOWACOURSE_CREW");
        String token = uniqueToken();
        List<Long> reactAndJava = techTagIds("react", "java");
        long react = reactAndJava.get(0);
        long java = reactAndJava.get(1);
        long reactJava6 = registerProject(author, teammate, token + " 리액트 자바", 6, reactAndJava);
        long java6 = registerProject(author, teammate, token + " 자바", 6, List.of(java));
        long java7 = registerProject(author, teammate, token + " 칠기 자바", 7, List.of(java));
        registerProject(author, teammate, token + " 승인 대기", 6, List.of(java));
        List.of(reactJava6, java6, java7).forEach(this::approve);

        Map<String, String> condition = Map.of("keyword", token, "cohorts", "6", "techTagIds", String.valueOf(java));
        Response options = findFilterOptions(condition);
        Response list = findAll(condition);

        assertThat(options.statusCode()).as(options.asString()).isEqualTo(200);
        assertThat(options.jsonPath().getLong("data.matchedProjectCount"))
                .isEqualTo(2)
                .isEqualTo(list.jsonPath().getLong("meta.totalCount"));
        assertThat(options.jsonPath().getInt("data.cohorts.find { it.cohort == 6 }.projectCount")).isEqualTo(2);
        assertThat(options.jsonPath().getInt("data.cohorts.find { it.cohort == 7 }.projectCount")).isEqualTo(1);
        assertThat(options.jsonPath().getInt("data.techTags.find { it.id == " + java + " }.projectCount")).isEqualTo(2);
        assertThat(options.jsonPath().getInt("data.techTags.find { it.id == " + react + " }.projectCount")).isEqualTo(1);
    }

    @Test
    @DisplayName("필터 옵션 조회에서 정의되지 않은 기수를 고르면 400을 반환한다.")
    void rejectsUndefinedCohortForFilterOptions() {
        Response response = findFilterOptions(Map.of("cohorts", "99"));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("code")).isEqualTo("INVALID_COHORT");
    }

    /**
     * 제목과 기수를 정해 프로젝트를 등록한다. 등록 직후에는 승인 대기(PENDING) 상태다.
     */
    private long registerProject(
            LoginSession author,
            LoginSession teammate,
            String title,
            int cohort,
            List<Long> techTagIds
    ) {
        Map<String, Object> body = new HashMap<>(
                requestBody(uniqueRepositoryName(), techTagIds, List.of(teammate.handle())));
        body.put("title", title);
        body.put("cohort", cohort);
        Response response = registerProject(author, body);
        assertThat(response.statusCode()).as(response.asString()).isEqualTo(201);
        return response.jsonPath().getLong("data.projectId");
    }

    /**
     * 비로그인으로 목록을 조회한다.
     */
    private Response findAll(Map<String, ?> queryParams) {
        return RestAssured.given()
                .port(port)
                .queryParams(queryParams)
                .when()
                .get(PROJECTS_PATH);
    }

    private Response findUserProjects(String handle) {
        return RestAssured.given()
                .port(port)
                .when()
                .get("/api/v1/users/{handle}/projects", handle);
    }

    /**
     * 비로그인으로 필터 옵션을 조회한다.
     */
    private Response findFilterOptions(Map<String, ?> queryParams) {
        return RestAssured.given()
                .port(port)
                .queryParams(queryParams)
                .when()
                .get(PROJECTS_PATH + "/filters");
    }

    private static String joinIds(List<Long> ids) {
        return String.join(",", ids.stream().map(String::valueOf).toList());
    }

    /**
     * 로컬 DB 를 다른 테스트와 함께 쓰므로, 제목에 넣고 검색어로 쓸 무작위 토큰으로 이 테스트의 프로젝트만 조회한다.
     */
    private static String uniqueToken() {
        return "tk" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private long registerPendingProject(LoginSession author, LoginSession teammate, List<Long> techTagIds) {
        Response response = registerProject(author, uniqueRepositoryName(), techTagIds, List.of(teammate.handle()));
        assertThat(response.statusCode()).as(response.asString()).isEqualTo(201);
        return response.jsonPath().getLong("data.projectId");
    }

    /**
     * 관리자 승인 기능이 아직 없으므로, DB 에서 승인 상태를 직접 바꾼다.
     */
    private void approve(long projectId) {
        jdbcTemplate.update("UPDATE projects SET approval_status = 'APPROVED' WHERE id = ?", projectId);
    }

    /**
     * viewer 가 null 이면 세션 쿠키 없이 비로그인으로 조회한다. 조회는 CSRF 토큰이 필요 없다.
     */
    private Response findDetail(LoginSession viewer, long projectId) {
        var request = RestAssured.given().port(port);
        if (viewer != null) {
            request.cookie("JSESSIONID", viewer.sessionId());
        }
        return request.when().get(PROJECTS_PATH + "/{projectId}", projectId);
    }

    private Response registerProject(
            LoginSession author,
            String repositoryName,
            List<Long> techTagIds,
            List<String> memberHandles
    ) {
        return registerProject(author, requestBody(repositoryName, techTagIds, memberHandles));
    }

    private Response registerProject(LoginSession author, Map<String, Object> body) {
        return RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", author.sessionId())
                .header("X-CSRF-Token", author.csrfToken())
                .contentType("application/json")
                .body(body)
                .when()
                .post(PROJECTS_PATH);
    }

    private static Map<String, Object> requestBody(
            String repositoryName,
            List<Long> techTagIds,
            List<String> memberHandles
    ) {
        return requestBodyWithUrl(
                "https://github.com/woowacourse-teams/" + repositoryName,
                techTagIds,
                memberHandles
        );
    }

    private static Map<String, Object> requestBodyWithUrl(
            String githubRepositoryUrl,
            List<Long> techTagIds,
            List<String> memberHandles
    ) {
        return Map.of(
                "title", "루프 (Loop)",
                "teamName", "루프팀",
                "tagline", "스프린트 회고와 액션 아이템을 하나로 엮은 실시간 협업 도구",
                "cohort", 6,
                "githubRepositoryUrl", githubRepositoryUrl,
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
                        "displayName", "크루"
                ))
                .when()
                .post("/api/v1/auth/signup");

        assertThat(signup.statusCode()).as(signup.asString()).isEqualTo(201);
        long userId = signup.jsonPath().getLong("data.userId");
        changeUserType(userId, userType);
        return new LoginSession(
                signup.cookie("JSESSIONID"),
                csrfToken,
                userId,
                handle
        );
    }

    /**
     * 가입하면 일반 사용자가 되고 크루 인증 기능은 아직 없으므로, DB 에서 사용자 종류를 직접 바꾼다.
     * 크루는 트랙과 기수가 필요하고, 코치는 기수를 가질 수 없다.
     */
    private void changeUserType(long userId, String userType) {
        if ("WOOWACOURSE_CREW".equals(userType)) {
            jdbcTemplate.update(
                    "UPDATE user_profiles SET user_type = 'WOOWACOURSE_CREW', track = 'BACKEND', cohort = 6 WHERE user_id = ?",
                    userId);
        }
        if ("WOOWACOURSE_COACH".equals(userType)) {
            jdbcTemplate.update(
                    "UPDATE user_profiles SET user_type = 'WOOWACOURSE_COACH' WHERE user_id = ?",
                    userId);
        }
    }

    private record LoginSession(String sessionId, String csrfToken, long userId, String handle) {
    }
}
