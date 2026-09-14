package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectMembersTest {

    private static final long REGISTERED_BY = 7L;

    @Test
    @DisplayName("등록자를 첫 팀원으로 두고, 팀원을 입력한 순서대로 이어 붙인다.")
    void placesRegistrantFirstAndKeepsMemberOrder() {
        ProjectMembers members = ProjectMembers.of(REGISTERED_BY, List.of(9L, 8L));

        assertThat(members.getUserIds()).containsExactly(REGISTERED_BY, 9L, 8L);
    }

    @Test
    @DisplayName("팀원 목록은 밖에서 바꿀 수 없다.")
    void returnsUnmodifiableUserIds() {
        ProjectMembers members = ProjectMembers.of(REGISTERED_BY, List.of(8L));

        assertThatThrownBy(() -> members.getUserIds().add(9L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("등록자 외 팀원이 없는 경우, 400 예외를 던진다.")
    void rejectsEmptyMembers() {
        assertInvalidMembers(List.of(), ProjectErrorCode.PROJECT_MEMBER_REQUIRED);
    }

    @Test
    @DisplayName("등록자 본인이 팀원 목록에 있는 경우, 400 예외를 던진다.")
    void rejectsRegistrantInMembers() {
        assertInvalidMembers(List.of(8L, REGISTERED_BY), ProjectErrorCode.PROJECT_MEMBER_INCLUDES_REGISTRANT);
    }

    @Test
    @DisplayName("같은 사용자가 두 번 있으면 400 예외를 던진다.")
    void rejectsDuplicateMembers() {
        assertInvalidMembers(List.of(8L, 9L, 8L), ProjectErrorCode.PROJECT_DUPLICATE_MEMBER);
    }

    private static void assertInvalidMembers(List<Long> memberIds, ProjectErrorCode expected) {
        assertThatThrownBy(() -> ProjectMembers.of(REGISTERED_BY, memberIds))
                .isInstanceOfSatisfying(InvalidProjectMemberException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(expected));
    }
}
