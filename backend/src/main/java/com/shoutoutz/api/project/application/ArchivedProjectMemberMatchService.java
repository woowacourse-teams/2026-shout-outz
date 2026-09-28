package com.shoutoutz.api.project.application;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GitHub OAuth 계정과 이전 기수 프로젝트의 참여자 스냅샷을 연결한다.
 *
 * <p>아카이브 참여자 원본은 유지하고, 현재 사용자와의 연결만
 * {@code matched_user_id}와 {@code project_members}에 반영한다.</p>
 */
@Service
@RequiredArgsConstructor
public class ArchivedProjectMemberMatchService {

    private static final String MATCH_ARCHIVED_MEMBERS_SQL = """
            UPDATE woowa_archived_project_members
            SET matched_user_id = ?
            WHERE matched_user_id IS NULL
              AND github_account_id = ?
            """;

    private static final String LINK_PROJECT_MEMBERS_SQL = """
            INSERT INTO project_members (project_id, user_id, display_order)
            SELECT am.project_id, ?, am.display_order
            FROM woowa_archived_project_members am
            WHERE am.matched_user_id = ?
            ON CONFLICT (project_id, user_id) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 같은 GitHub 계정으로 연결된 모든 아카이브 참여자를 현재 사용자와 연결한다.
     * 가입/로그인 트랜잭션 안에서 호출되므로 두 테이블의 변경도 함께 커밋된다.
     */
    @Transactional
    public void matchGithubAccount(long userId, String githubAccountId) {
        if (userId <= 0 || githubAccountId == null || githubAccountId.isBlank()) {
            return;
        }

        jdbcTemplate.update(MATCH_ARCHIVED_MEMBERS_SQL, userId, githubAccountId);
        jdbcTemplate.update(LINK_PROJECT_MEMBERS_SQL, userId, userId);
    }
}
