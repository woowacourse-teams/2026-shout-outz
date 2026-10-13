package com.shoutoutz.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "loadtest"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoadTestLoginAcceptanceTest {

    private static final String SESSION_COOKIE = "JSESSIONID";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String handle;
    private Long userId;

    @BeforeEach
    void setUp() {
        handle = "lt-" + UUID.randomUUID().toString().substring(0, 8);
        userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (handle, status, role) VALUES (?, 'ACTIVE', 'USER') RETURNING id",
                Long.class,
                "@" + handle
        );
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    @DisplayName("로그인하면 세션 쿠키가 발급되고, 그 쿠키로 로그인 상태가 조회된다.")
    void issuesAuthenticatedSessionCookie() {
        Response login = RestAssured.given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(Map.of("handle", handle))
                .post("/loadtest/login");

        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.getCookie(SESSION_COOKIE)).isNotBlank();
        assertThat(login.jsonPath().getString("data.status")).isEqualTo("AUTHENTICATED");
        assertThat(login.jsonPath().getLong("data.userId")).isEqualTo(userId);

        Response session = RestAssured.given()
                .port(port)
                .cookie(SESSION_COOKIE, login.getCookie(SESSION_COOKIE))
                .get("/api/v1/auth/session");

        assertThat(session.statusCode()).isEqualTo(200);
        assertThat(session.jsonPath().getString("data.status")).isEqualTo("AUTHENTICATED");
        assertThat(session.jsonPath().getLong("data.userId")).isEqualTo(userId);
    }

    @Test
    @DisplayName("handle이 비어 있으면 400을 반환한다.")
    void rejectsBlankHandle() {
        Response response = RestAssured.given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(Map.of("handle", " "))
                .post("/loadtest/login");

        assertThat(response.statusCode()).isEqualTo(400);
    }
}
