package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.Project;

/**
 * 프로젝트 수정 응답 객체
 * 수정으로 승인 상태가 바뀔 수 있어, 바뀐 상태를 함께 돌려준다.
 */
public record ProjectUpdateResponse(Long projectId, ApprovalStatus approvalStatus) {

    public static ProjectUpdateResponse from(Project project) {
        return new ProjectUpdateResponse(project.getId(), project.getApprovalStatus());
    }
}
