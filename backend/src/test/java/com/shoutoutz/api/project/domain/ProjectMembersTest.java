package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.project.domain.exception.InvalidProjectMemberException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectMembersTest {

    private static final long REGISTERED_BY = 7L;

    @Test
    @DisplayName("요청한 팀원을 입력한 순서대로 보관한다.")
    void keepsMemberOrder() {
        ProjectMembers members = ProjectMembers.of(List.of(9L, 8L));

        assertThat(members.getUserIds()).containsExactly(9L, 8L);
    }

    @Test
    @DisplayName("팀원 목록은 밖에서 바꿀 수 없다.")
    void returnsUnmodifiableUserIds() {
        ProjectMembers members = ProjectMembers.of(List.of(8L));

        assertThatThrownBy(() -> members.getUserIds().add(9L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("팀원이 없는 경우, 400 예외를 던진다.")
    void rejectsEmptyMembers() {
        assertInvalidMembers(List.of(), ProjectErrorCode.PROJECT_MEMBER_REQUIRED);
    }

    @Test
    @DisplayName("등록자를 포함해 요청한 팀원을 그대로 보관한다.")
    void acceptsRegistrantInMembers() {
        ProjectMembers members = ProjectMembers.of(List.of(8L, REGISTERED_BY));

        assertThat(members.getUserIds()).containsExactly(8L, REGISTERED_BY);
    }

    @Test
    @DisplayName("같은 사용자가 두 번 있으면 400 예외를 던진다.")
    void rejectsDuplicateMembers() {
        assertInvalidMembers(List.of(8L, 9L, 8L), ProjectErrorCode.PROJECT_DUPLICATE_MEMBER);
    }

    private static void assertInvalidMembers(List<Long> memberIds, ProjectErrorCode expected) {
        assertThatThrownBy(() -> ProjectMembers.of(memberIds))
                .isInstanceOfSatisfying(InvalidProjectMemberException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(expected));
    }
}
