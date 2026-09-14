package com.shoutoutz.api.comment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.comment.domain.ProjectComment;
import com.shoutoutz.api.comment.domain.ProjectCommentRepository;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.TeamName;
import com.shoutoutz.api.project.domain.Title;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ProjectCommentRepositoryIntegrationTest {

    @Autowired
    private ProjectCommentRepository projectCommentRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("프로젝트 댓글을 저장하고 생성·수정 시각과 함께 조회한다")
    void savesAndFindsProjectComment() {
        User author = userRepository.save(User.initialize("comment-repo-" + uniqueSuffix()));
        Project project = projectRepository.save(
                project(author.getId()),
                List.of(),
                List.of()
        );

        ProjectComment saved = projectCommentRepository.save(
                ProjectComment.create(project.getId(), author.getId(), null, "  댓글 내용  ")
        );

        ProjectComment found = projectCommentRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getProjectId()).isEqualTo(project.getId());
        assertThat(found.getAuthorId()).isEqualTo(author.getId());
        assertThat(found.getParentId()).isNull();
        assertThat(found.getContent()).isEqualTo("댓글 내용");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("기존 프로젝트 댓글의 내용을 수정하고 생성 시각은 유지한다")
    void updatesProjectComment() {
        User author = userRepository.save(User.initialize("comment-update-" + uniqueSuffix()));
        Project project = projectRepository.save(
                project(author.getId()),
                List.of(),
                List.of()
        );

        ProjectComment saved = projectCommentRepository.save(
                ProjectComment.create(project.getId(), author.getId(), null, "기존 댓글")
        );

        ProjectComment updated = projectCommentRepository.save(saved.updateContent("수정된 댓글"));
        ProjectComment found = projectCommentRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getContent()).isEqualTo("수정된 댓글");
        assertThat(found.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(found.getUpdatedAt()).isEqualTo(updated.getUpdatedAt());
        assertThat(found.getUpdatedAt()).isNotEqualTo(found.getCreatedAt());
    }

    private static Project project(Long registeredBy) {
        String repositoryName = "2026-comment-" + uniqueSuffix();
        return Project.register(
                Cohort.COHORT_8,
                registeredBy,
                new TeamName("댓글 저장팀"),
                new Title("댓글 저장 프로젝트"),
                "댓글 저장을 검증한다",
                "설명",
                new GithubRepositoryUrl("https://github.com/woowacourse-teams/" + repositoryName),
                null,
                null
        );
    }

    private static String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
