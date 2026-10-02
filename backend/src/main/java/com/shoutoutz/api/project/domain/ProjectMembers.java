package com.shoutoutz.api.project.domain;

import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_DUPLICATE_MEMBER;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_MEMBER_REQUIRED;

import com.shoutoutz.api.project.domain.exception.InvalidProjectMemberException;
import java.util.HashSet;
import java.util.List;
import lombok.Getter;

/**
 * 프로젝트 팀원 목록
 * 요청한 순서대로 팀원을 보관한다.
 * 팀 프로젝트만 등록할 수 있으므로, 팀원이 한 명 이상 있어야 한다.
 * 규칙을 거치지 않고 만들어지지 않도록, 생성은 of 로만 한다.
 */
@Getter
public final class ProjectMembers {

    private final List<Long> userIds;

    private ProjectMembers(List<Long> userIds) {
        this.userIds = List.copyOf(userIds);
    }

    public static ProjectMembers of(List<Long> memberIds) {
        validateNotEmpty(memberIds);
        validateNotDuplicated(memberIds);
        return new ProjectMembers(memberIds);
    }

    private static void validateNotEmpty(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            throw new InvalidProjectMemberException(PROJECT_MEMBER_REQUIRED);
        }
    }

    private static void validateNotDuplicated(List<Long> memberIds) {
        if (new HashSet<>(memberIds).size() != memberIds.size()) {
            throw new InvalidProjectMemberException(PROJECT_DUPLICATE_MEMBER);
        }
    }
}
