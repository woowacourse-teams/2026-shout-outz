package com.shoutoutz.api.project.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectErrorCode implements ErrorCode {
    /**
     * 프로젝트 식별자 (Slug) 에러 코드
     */
    PROJECT_INVALID_SLUG("리포지토리 이름으로 프로젝트 주소를 만들 수 없습니다. 소문자와 숫자, 하이픈만 사용할 수 있습니다."),
    PROJECT_DUPLICATE_SLUG("이미 등록된 리포지토리입니다."),

    /**
     * 프로젝트 링크 에러 코드
     */
    PROJECT_INVALID_GITHUB_REPOSITORY_URL("GitHub 리포지토리 URL 형식이 올바르지 않습니다."),
    PROJECT_INVALID_DEPLOYMENT_URL("서비스 배포 URL 형식이 올바르지 않습니다."),

    /**
     * 프로젝트 기술 스택 에러 코드
     */
    PROJECT_DUPLICATE_TECH_TAG("기술 스택에 중복된 항목이 있습니다."),
    PROJECT_INVALID_TECH_TAG("선택할 수 없는 기술 스택이 포함되어 있습니다."),

    /**
     * 프로젝트 썸네일 에러 코드
     */
    PROJECT_INVALID_THUMBNAIL("프로젝트 썸네일로 사용할 수 없는 이미지입니다."),
    PROJECT_THUMBNAIL_NOT_READY("썸네일 이미지를 아직 처리하고 있습니다. 잠시 후 다시 시도해주세요."),

    /**
     * 프로젝트 기본 정보 에러 코드
     */
    PROJECT_TITLE_NULL_OR_BLANK("프로젝트 이름은 null이거나 빈값일 수 없습니다."),
    PROJECT_INVALID_TITLE_LENGTH("프로젝트 이름은 100자 이하여야 합니다."),

    PROJECT_TEAM_NAME_NULL_OR_BLANK("팀 이름은 null이거나 빈값일 수 없습니다."),
    PROJECT_INVALID_TEAM_NAME_LENGTH("팀 이름은 50자 이하여야 합니다."),

    PROJECT_TAGLINE_NULL_OR_BLANK("한 줄 소개는 null이거나 빈값일 수 없습니다."),
    PROJECT_INVALID_TAGLINE_LENGTH("한 줄 소개는 200자 이하여야 합니다."),
    PROJECT_INVALID_DESCRIPTION_LENGTH("프로젝트 설명은 100000자 이하여야 합니다."),

    PROJECT_COHORT_NULL("프로젝트 기수는 null일 수 없습니다."),
    PROJECT_REGISTERED_BY_NULL("프로젝트 등록자는 null일 수 없습니다.");

    private final String message;
}
