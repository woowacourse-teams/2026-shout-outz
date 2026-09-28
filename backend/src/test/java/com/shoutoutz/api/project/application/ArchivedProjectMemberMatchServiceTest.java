package com.shoutoutz.api.project.application;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ArchivedProjectMemberMatchServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private ArchivedProjectMemberMatchService service;

    @BeforeEach
    void setUp() {
        service = new ArchivedProjectMemberMatchService(jdbcTemplate);
    }

    @Test
    void matchesArchivedMembersAndLinksCurrentProjectMembers() {
        service.matchGithubAccount(7L, "12345678");

        verify(jdbcTemplate).update(anyString(), eq(7L), eq("12345678"));
        verify(jdbcTemplate).update(anyString(), eq(7L), eq(7L));
    }

    @Test
    void ignoresInvalidIdentity() {
        service.matchGithubAccount(0L, "12345678");
        service.matchGithubAccount(7L, " ");

        verifyNoInteractions(jdbcTemplate);
    }
}
