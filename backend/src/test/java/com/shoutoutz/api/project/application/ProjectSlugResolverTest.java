package com.shoutoutz.api.project.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.Slug;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectSlugResolverTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectSlugResolver projectSlugResolver;

    @Test
    @DisplayName("slug 로 찾은 프로젝트 id 를 돌려준다")
    void resolvesProjectId() {
        given(projectRepository.findIdBySlug(new Slug("loop"))).willReturn(Optional.of(7L));

        assertThat(projectSlugResolver.resolveId("loop")).isEqualTo(7L);
    }

    @Test
    @DisplayName("없는 slug 면 404 예외를 던진다")
    void rejectsUnknownSlug() {
        given(projectRepository.findIdBySlug(new Slug("unknown"))).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectSlugResolver.resolveId("unknown"))
                .isInstanceOfSatisfying(EntityNotFoundException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Loop", "my_app", "-loop", ""})
    @DisplayName("형식이 틀린 slug 는 조회하지 않고 404 예외를 던진다")
    void rejectsMalformedSlugWithoutQuery(String slug) {
        assertThatThrownBy(() -> projectSlugResolver.resolveId(slug))
                .isInstanceOfSatisfying(EntityNotFoundException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
        verify(projectRepository, never()).findIdBySlug(any());
    }
}
