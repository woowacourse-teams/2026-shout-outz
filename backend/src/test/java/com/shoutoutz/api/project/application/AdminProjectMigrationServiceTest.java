package com.shoutoutz.api.project.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.ValidationFailedException;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.infrastructure.AdminProjectMigrationRepository;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectMigrationUpdateResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AdminProjectMigrationServiceTest {

    private static final long PROJECT_ID = 100L;
    private static final long ADMIN_ID = 7L;
    private static final Instant NOW = Instant.parse("2026-09-30T00:00:00Z");

    @Mock
    private AdminProjectMigrationRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void correctsImportedFieldsAndTechTagsInOneRequest() throws Exception {
        AdminProjectMigrationService service = new AdminProjectMigrationService(repository);
        given(repository.update(eq(PROJECT_ID), anyMap())).willReturn(Optional.of(NOW));
        JsonNode request = objectMapper.readTree("""
                {
                  "title": "수정된 프로젝트",
                  "descriptionMd": "![화면](https://github.com/user-attachments/assets/example)",
                  "starCount": 42,
                  "deploymentUrl": null,
                  "techTagIds": [3, 1]
                }
                """);

        AdminProjectMigrationUpdateResponse response = service.update(
                PROJECT_ID, ADMIN_ID, UserRole.ADMIN, request
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> values = ArgumentCaptor.forClass(Map.class);
        verify(repository).update(eq(PROJECT_ID), values.capture());
        assertThat(values.getValue()).containsEntry("title", "수정된 프로젝트")
                .containsEntry("star_count", 42)
                .containsEntry("description_md", "![화면](https://github.com/user-attachments/assets/example)")
                .containsKey("deployment_url");
        assertThat(values.getValue().get("deployment_url")).isNull();
        verify(repository).replaceTechTags(PROJECT_ID, List.of(3L, 1L));
        assertThat(response.updatedFields())
                .containsExactly("title", "descriptionMd", "starCount", "deploymentUrl", "techTagIds");
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void replacesOnlyTechTagsWhenNoProjectFieldIsProvided() throws Exception {
        AdminProjectMigrationService service = new AdminProjectMigrationService(repository);
        given(repository.update(eq(PROJECT_ID), anyMap())).willReturn(Optional.of(NOW));

        service.update(PROJECT_ID, ADMIN_ID, UserRole.ADMIN,
                objectMapper.readTree("{\"techTagIds\":[]}"));

        verify(repository).update(PROJECT_ID, Map.of());
        verify(repository).replaceTechTags(PROJECT_ID, List.of());
    }

    @Test
    void rejectsNonAdminBeforeAccessingRepository() throws Exception {
        AdminProjectMigrationService service = new AdminProjectMigrationService(repository);

        assertThatThrownBy(() -> service.update(PROJECT_ID, ADMIN_ID, UserRole.USER,
                objectMapper.readTree("{\"title\":\"수정\"}")))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ProjectErrorCode.PROJECT_MIGRATION_ADMIN_FORBIDDEN));
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsFieldsOutsideImportedProjectAndTags() throws Exception {
        AdminProjectMigrationService service = new AdminProjectMigrationService(repository);

        assertThatThrownBy(() -> service.update(PROJECT_ID, ADMIN_ID, UserRole.ADMIN,
                objectMapper.readTree("{\"memberHandles\":[\"@member\"]}")))
                .isInstanceOfSatisfying(ValidationFailedException.class,
                        error -> assertThat(error.getDetails().getFirst().field()).isEqualTo("memberHandles"));
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsDuplicateTagsWithoutWriting() throws Exception {
        AdminProjectMigrationService service = new AdminProjectMigrationService(repository);

        assertThatThrownBy(() -> service.update(PROJECT_ID, ADMIN_ID, UserRole.ADMIN,
                objectMapper.readTree("{\"techTagIds\":[1,1]}")))
                .isInstanceOf(ValidationFailedException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void returnsNotFoundForNonArchivedProject() throws Exception {
        AdminProjectMigrationService service = new AdminProjectMigrationService(repository);
        given(repository.update(eq(PROJECT_ID), anyMap())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(PROJECT_ID, ADMIN_ID, UserRole.ADMIN,
                objectMapper.readTree("{\"title\":\"수정\"}")))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
