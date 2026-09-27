package com.shoutoutz.api.comment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.comment.domain.ProjectCommentReactionCounts;
import com.shoutoutz.api.comment.domain.ProjectCommentReactionRepository;
import com.shoutoutz.api.comment.domain.ProjectCommentReactionType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ProjectCommentReactionRepositoryIntegrationTest {

    @Autowired
    private ProjectCommentReactionRepository projectCommentReactionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 같은_사용자의_같은_댓글_공감을_중복_추가해도_한_건만_저장한다() {
        long firstUserId = insertUser();
        long secondUserId = insertUser();
        long projectId = insertProject(firstUserId);
        long commentId = insertComment(projectId, firstUserId);

        projectCommentReactionRepository.add(commentId, firstUserId, ProjectCommentReactionType.AGREE);
        projectCommentReactionRepository.add(commentId, firstUserId, ProjectCommentReactionType.AGREE);
        projectCommentReactionRepository.add(commentId, secondUserId, ProjectCommentReactionType.AGREE);

        assertThat(projectCommentReactionRepository.countByCommentId(commentId))
                .isEqualTo(new ProjectCommentReactionCounts(2L));

        projectCommentReactionRepository.remove(commentId, firstUserId, ProjectCommentReactionType.AGREE);
        projectCommentReactionRepository.remove(commentId, firstUserId, ProjectCommentReactionType.AGREE);

        assertThat(projectCommentReactionRepository.countByCommentId(commentId))
                .isEqualTo(new ProjectCommentReactionCounts(1L));
    }

    private long insertUser() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (handle) VALUES (?) RETURNING id",
                Long.class,
                "comment_react_" + token
        );
    }

    private long insertProject(long registeredBy) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO projects (
                            cohort, registered_by, team_name, slug, title, tagline,
                            github_repository_url, service_status, approval_status
                        ) VALUES (
                            6, ?, '댓글 반응 테스트팀', ?, '댓글 반응 테스트 프로젝트', '한 줄 소개',
                            ?, 'CLOSED', 'APPROVED'
                        )
                        RETURNING id
                        """,
                Long.class,
                registeredBy,
                "comment-reaction-" + token,
                "https://github.com/woowacourse-teams/comment-reaction-" + token
        );
    }

    private long insertComment(long projectId, long authorId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO project_comments (project_id, author_id, content) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                projectId,
                authorId,
                "공감할 댓글"
        );
    }
}
