package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ProjectTechTag;

/**
 * 프로젝트 기술 스택 응답 객체
 * 상세 조회와 목록 조회 응답이 함께 쓴다.
 */
public record ProjectTechTagResponse(long id, String displayName) {

    public static ProjectTechTagResponse from(ProjectTechTag techTag) {
        return new ProjectTechTagResponse(techTag.id(), techTag.displayName());
    }
}
