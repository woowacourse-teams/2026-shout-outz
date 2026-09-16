package com.shoutoutz.api.project.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.project.application.ProjectCursorCodec;
import com.shoutoutz.api.project.domain.ProjectCursor;
import com.shoutoutz.api.project.domain.ProjectSort;
import com.shoutoutz.api.project.domain.exception.InvalidProjectCursorException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserProjectFindRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("파라미터를 입력하지 않으면 최신순 첫 페이지를 20개 조회한다.")
    void usesDefaultsWhenAbsent() {
        UserProjectFindRequest request = new UserProjectFindRequest(null, null);

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.resolvedSize()).isEqualTo(20);
        assertThat(request.resolvedCursor()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 50})
    @DisplayName("조회 개수는 1 이상 50 이하로 입력할 수 있다.")
    void acceptsSizeInRange(int size) {
        UserProjectFindRequest request = new UserProjectFindRequest(size, null);

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.resolvedSize()).isEqualTo(size);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 51})
    @DisplayName("조회 개수가 범위를 벗어나면 검증에 실패한다.")
    void rejectsSizeOutOfRange(int size) {
        UserProjectFindRequest request = new UserProjectFindRequest(size, null);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("공백뿐인 커서는 첫 페이지로 본다.")
    void ignoresBlankCursor() {
        UserProjectFindRequest request = new UserProjectFindRequest(null, " ");

        assertThat(request.cursor()).isNull();
        assertThat(request.resolvedCursor()).isNull();
    }

    @Test
    @DisplayName("최신순 프로젝트 커서를 해독한다.")
    void resolvesLatestCursor() {
        ProjectCursor cursor = ProjectCursor.latest(Instant.parse("2026-09-15T00:00:00Z"), 10L);
        UserProjectFindRequest request = new UserProjectFindRequest(10, ProjectCursorCodec.encode(cursor));

        assertThat(request.resolvedCursor()).isEqualTo(cursor);
    }

    @Test
    @DisplayName("인기순 커서는 사용자 프로젝트 목록에 사용할 수 없다.")
    void rejectsPopularCursor() {
        ProjectCursor cursor = ProjectCursor.popular(3L, Instant.parse("2026-09-15T00:00:00Z"), 10L);
        UserProjectFindRequest request = new UserProjectFindRequest(10, ProjectCursorCodec.encode(cursor));

        assertThatThrownBy(request::resolvedCursor).isInstanceOf(InvalidProjectCursorException.class);
    }
}
