package com.shoutoutz.api.comment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.comment.application.ProjectCommentQueryRepository;
import com.shoutoutz.api.comment.application.dto.ProjectCommentCursor;
import com.shoutoutz.api.comment.application.dto.ProjectCommentPage;
import com.shoutoutz.api.comment.domain.ProjectComment;
import com.shoutoutz.api.comment.domain.ProjectCommentRepository;
import com.shoutoutz.api.comment.domain.ProjectCommentSort;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.TeamName;
import com.shoutoutz.api.project.domain.Title;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import java.time.Instant;
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
    private ProjectCommentQueryRepository projectCommentQueryRepository;

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

    @Test
    @DisplayName("프로젝트 댓글을 soft delete하면 삭제·수정 시각을 갱신하고 기존 대댓글을 보존한다")
    void softDeletesProjectComment() {
        User author = userRepository.save(User.initialize("comment-delete-" + uniqueSuffix()));
        Project project = projectRepository.save(
                project(author.getId()),
                List.of(),
                List.of()
        );

        ProjectComment root = projectCommentRepository.save(
                ProjectComment.create(project.getId(), author.getId(), null, "삭제할 루트 댓글")
        );
        ProjectComment reply = projectCommentRepository.save(
                ProjectComment.create(project.getId(), author.getId(), root.getId(), "기존 대댓글")
        );

        ProjectComment deleted = projectCommentRepository.save(root.delete(Instant.now()));
        ProjectComment found = projectCommentRepository.findById(root.getId()).orElseThrow();

        assertThat(deleted.isDeleted()).isTrue();
        assertThat(found.isDeleted()).isTrue();
        assertThat(found.getDeletedAt()).isNotNull();
        assertThat(found.getContent()).isEqualTo("삭제할 루트 댓글");
        assertThat(found.getUpdatedAt()).isAfter(root.getUpdatedAt());

        ProjectCommentPage page = projectCommentQueryRepository.findRootCommentsPage(
                project.getId(),
                null,
                ProjectCommentSort.LATEST,
                10
        );
        assertThat(page.comments()).extracting(ProjectComment::getId)
                .contains(root.getId());
        assertThat(projectCommentQueryRepository.findReplies(
                project.getId(),
                List.of(root.getId())
        )).extracting(ProjectComment::getId).containsExactly(reply.getId());
    }

    @Test
    @DisplayName("루트 댓글을 정렬 기준과 크기에 따라 조회하고 대댓글을 부모 ID로 조회한다")
    void findsRootPageAndReplies() {
        User author = userRepository.save(User.initialize("comment-list-" + uniqueSuffix()));
        Project project = projectRepository.save(
                project(author.getId()),
                List.of(),
                List.of()
        );

        ProjectComment firstRoot = projectCommentRepository.save(
                ProjectComment.create(project.getId(), author.getId(), null, "첫 번째 루트")
        );
        ProjectComment reply = projectCommentRepository.save(
                ProjectComment.create(project.getId(), author.getId(), firstRoot.getId(), "첫 번째 대댓글")
        );
        ProjectComment secondRoot = projectCommentRepository.save(
                ProjectComment.create(project.getId(), author.getId(), null, "두 번째 루트")
        );

        ProjectCommentPage page = projectCommentQueryRepository.findRootCommentsPage(
                project.getId(),
                null,
                ProjectCommentSort.LATEST,
                1
        );

        assertThat(page.comments()).hasSize(1);
        assertThat(page.comments().getFirst().getId()).isEqualTo(secondRoot.getId());
        assertThat(page.hasNext()).isTrue();

        ProjectCommentPage nextPage = projectCommentQueryRepository.findRootCommentsPage(
                project.getId(),
                new ProjectCommentCursor(
                        page.comments().getFirst().getCreatedAt(),
                        page.comments().getFirst().getId(),
                        ProjectCommentSort.LATEST
                ),
                ProjectCommentSort.LATEST,
                1
        );
        assertThat(nextPage.comments()).extracting(ProjectComment::getId)
                .containsExactly(firstRoot.getId());

        ProjectCommentPage oldestPage = projectCommentQueryRepository.findRootCommentsPage(
                project.getId(),
                null,
                ProjectCommentSort.OLDEST,
                1
        );
        assertThat(oldestPage.comments().getFirst().getId()).isEqualTo(firstRoot.getId());
        assertThat(projectCommentQueryRepository.findReplies(
                project.getId(),
                List.of(firstRoot.getId())
        )).extracting(ProjectComment::getId).containsExactly(reply.getId());
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
