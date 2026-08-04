package com.dropit.backend;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@Sql(scripts = "/test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ApiSmokeIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("supabase.jwt.secret", () -> "test-secret-that-is-long-enough-for-hs256");
        registry.add("supabase.jwt.issuer", () -> "test-issuer");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @BeforeEach
    void cleanData() {
        jdbcTemplate.update("delete from public.site_visitors");
        jdbcTemplate.update("delete from public.app_comments");
        jdbcTemplate.update("delete from public.app_bookmarks");
        jdbcTemplate.update("delete from public.app_likes");
        jdbcTemplate.update("delete from public.apps");
        jdbcTemplate.update("delete from public.makers");
        jdbcTemplate.update("delete from public.crew_members");
    }

    @Test
    void publicAppListWorksAgainstPostgres() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String snapshot = "{\"id\":\"" + ownerId + "\",\"name\":\"샤를\",\"initials\":\"샤\",\"role\":\"BE\",\"bio\":\"소개\",\"tone\":\"#d9e6ff\"}";
        jdbcTemplate.update("""
            insert into public.apps (
                id, owner_id, name, tagline, description, category, categories,
                thumbnail_variant, app_url, maker, tech_tags, source
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
            """,
            "app-smoke", ownerId, "Drop Smoke", "drop search", "설명", "개발",
            new String[]{"개발"}, "new", "https://example.com", snapshot,
            new String[]{"Spring", "JPA"}, "submitted"
        );

        mockMvc.perform(get("/v1/apps")
                .param("category", "개발")
                .param("q", "drop")
                .param("sort", "latest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].id").value("app-smoke"))
            .andExpect(jsonPath("$.items[0].categories[0]").value("개발"));
    }

    @Test
    void siteVisitIsIdempotentForTheSameVisitorAndDay() throws Exception {
        String body = "{\"visitorId\":\"visitor-smoke\"}";

        mockMvc.perform(post("/v1/site-visits")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dailyVisitors").value(1))
            .andExpect(jsonPath("$.totalVisitors").value(1));

        mockMvc.perform(post("/v1/site-visits")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dailyVisitors").value(1))
            .andExpect(jsonPath("$.totalVisitors").value(1));
    }
}
