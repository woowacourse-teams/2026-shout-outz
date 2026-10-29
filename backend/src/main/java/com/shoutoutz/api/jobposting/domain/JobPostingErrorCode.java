package com.shoutoutz.api.jobposting.domain;

import com.shoutoutz.api.common.exception.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobPostingErrorCode implements ErrorCode {
    /**
     * 채용공고 (JobPosting) 에러 코드
     */
    JOB_POSTING_NOT_FOUND("채용공고를 찾을 수 없습니다."),
    JOB_POSTING_ADMIN_FORBIDDEN("관리자만 채용공고를 관리할 수 있습니다."),
    JOB_POSTING_DUPLICATE_URL("이미 등록된 채용공고 주소입니다."),
    JOB_POSTING_URL_MISMATCH("채용공고 주소는 바꿀 수 없습니다. 다른 주소의 공고는 새로 등록해주세요."),
    JOB_POSTING_EXTRACTION_FAILED("채용공고에서 정보를 뽑지 못했습니다. 잠시 후 다시 시도해주세요."),
    JOB_POSTING_ADMIN_CURSOR_INVALID("채용공고 목록 조회 커서가 올바르지 않습니다. 커서 없이 다시 조회해주세요."),

    JOB_POSTING_INVALID_ID_SIZE("채용공고 ID는 0보다 커야 합니다."),
    JOB_POSTING_INVALID_URL("채용공고 주소는 http 또는 https로 시작하는 1024자 이하의 주소여야 합니다."),
    JOB_POSTING_CONTENT_NULL_OR_BLANK("채용공고 원문은 null이거나 빈값일 수 없습니다."),
    JOB_POSTING_INVALID_CONTENT_HASH("채용공고 원문 해시는 SHA-256 hex 64자여야 합니다."),

    JOB_POSTING_COMPANY_NAME_NULL_OR_BLANK("회사명은 null이거나 빈값일 수 없습니다."),
    JOB_POSTING_INVALID_COMPANY_NAME_LENGTH("회사명은 100자 이하여야 합니다."),
    JOB_POSTING_JOB_ROLE_NULL("직무는 null일 수 없습니다."),
    JOB_POSTING_INVALID_CAREER_REQUIREMENT("경력 조건은 50자 이하여야 합니다."),
    JOB_POSTING_TITLE_NULL_OR_BLANK("공고 제목은 null이거나 빈값일 수 없습니다."),
    JOB_POSTING_INVALID_TITLE_LENGTH("공고 제목은 200자 이하여야 합니다."),
    JOB_POSTING_INVALID_EMPLOYMENT_TYPE("고용 형태는 30자 이하여야 합니다."),
    JOB_POSTING_STATUS_NULL("채용공고 매칭 상태는 null일 수 없습니다."),
    JOB_POSTING_REGISTERED_BY_NULL("채용공고 등록자 ID는 null일 수 없습니다."),
    JOB_POSTING_INVALID_REGISTERED_BY_SIZE("채용공고 등록자 ID는 0보다 커야 합니다."),
    JOB_POSTING_CREATED_AT_NULL("채용공고 생성 시각은 null일 수 없습니다."),
    JOB_POSTING_UPDATED_AT_NULL("채용공고 수정 시각은 null일 수 없습니다."),

    JOB_POSTING_INVALID_REQUIREMENT_COUNT("역량 항목은 1개 이상 50개 이하여야 합니다."),
    JOB_POSTING_INVALID_REQUIREMENT_ORDER("역량 항목의 순서는 1부터 빠짐없이 이어져야 합니다."),

    JOB_REQUIREMENT_INVALID_ID_SIZE("역량 항목 ID는 0보다 커야 합니다."),
    JOB_REQUIREMENT_NAME_NULL_OR_BLANK("역량 이름은 null이거나 빈값일 수 없습니다."),
    JOB_REQUIREMENT_INVALID_NAME_LENGTH("역량 이름은 100자 이하여야 합니다."),
    JOB_REQUIREMENT_CATEGORY_NULL("역량 구분은 null일 수 없습니다."),
    JOB_REQUIREMENT_SUMMARY_NULL_OR_BLANK("역량 요약은 null이거나 빈값일 수 없습니다."),
    JOB_REQUIREMENT_INVALID_SUMMARY_LENGTH("역량 요약은 500자 이하여야 합니다."),
    JOB_REQUIREMENT_STATUS_NULL("역량 항목 매칭 상태는 null일 수 없습니다.");

    private final String message;
}
