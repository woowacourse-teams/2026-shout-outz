package com.shoutoutz.api.project.domain;

import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_DUPLICATE_MEMBER;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_MEMBER_INCLUDES_REGISTRANT;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_MEMBER_REQUIRED;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import lombok.Getter;

/**
 * 프로젝트 팀원 목록
 * 등록자를 0에 두고, 팀원을 입력한 순서대로 이어 붙인다.
 * 팀 프로젝트만 등록할 수 있으므로, 등록자 외 팀원이 한 명 이상 있어야 한다.
 * 규칙을 거치지 않고 만들어지지 않도록, 생성은 of 로만 한다.
 */
@Getter
public final class ProjectMembers {

    private final List<Long> userIds;

    private ProjectMembers(List<Long> userIds) {
        this.userIds = List.copyOf(userIds);
    }

    public static ProjectMembers of(Long registeredBy, List<Long> memberIds) {
        validateNotEmpty(memberIds);
        validateNotIncludesRegistrant(registeredBy, memberIds);
        validateNotDuplicated(memberIds);

        List<Long> userIds = new ArrayList<>();
        userIds.add(registeredBy);
        userIds.addAll(memberIds);
        return new ProjectMembers(userIds);
    }

    private static void validateNotEmpty(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            throw new InvalidProjectMemberException(PROJECT_MEMBER_REQUIRED);
        }
    }

    /**
     * 등록자는 0번 팀원으로 자동으로 들어가므로, 팀원 목록에 다시 넣을 수 없다.
     */
    private static void validateNotIncludesRegistrant(Long registeredBy, List<Long> memberIds) {
        if (memberIds.contains(registeredBy)) {
            throw new InvalidProjectMemberException(PROJECT_MEMBER_INCLUDES_REGISTRANT);
        }
    }

    private static void validateNotDuplicated(List<Long> memberIds) {
        if (new HashSet<>(memberIds).size() != memberIds.size()) {
            throw new InvalidProjectMemberException(PROJECT_DUPLICATE_MEMBER);
        }
    }
}
