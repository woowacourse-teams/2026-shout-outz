package com.shoutoutz.api.jobposting.presentation.dto.request;

import com.shoutoutz.api.common.util.DataResolveUtil;
import com.shoutoutz.api.jobposting.domain.JobPostingUrl;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.validator.constraints.CodePointLength;

/**
 * 운영자가 추출 결과를 확인하고 고친 공고 정보와 역량 목록. 원문은 해시를 만드는 데만 쓰고 저장하지 않는다.
 * 길이 제한은 job_postings, job_requirements 테이블의 칸 길이와 같다.
 */
public record AdminJobPostingSaveRequest(
        @NotBlank(message = "sourceUrl은 필수입니다.")
        @CodePointLength(max = JobPostingUrl.MAX_LENGTH, message = "sourceUrl은 1,024자를 초과할 수 없습니다.")
        @Pattern(regexp = JobPostingUrl.REGEX, message = "sourceUrl은 http 또는 https URL 형식이어야 합니다.")
        String sourceUrl,

        @NotNull(message = "content는 필수입니다.")
        @Pattern(regexp = HAS_TEXT, message = "content는 필수입니다.")
        @CodePointLength(max = MAX_CONTENT_LENGTH, message = "content는 10,000자를 초과할 수 없습니다.")
        String content,

        @NotNull(message = "companyName은 필수입니다.")
        @Pattern(regexp = HAS_TEXT, message = "companyName은 필수입니다.")
        @CodePointLength(max = 100, message = "companyName은 100자를 초과할 수 없습니다.")
        String companyName,

        @NotNull(message = "jobRole은 필수입니다.")
        JobRole jobRole,

        @CodePointLength(max = 50, message = "careerRequirement는 50자를 초과할 수 없습니다.")
        String careerRequirement,

        @NotNull(message = "title은 필수입니다.")
        @Pattern(regexp = HAS_TEXT, message = "title은 필수입니다.")
        @CodePointLength(max = 200, message = "title은 200자를 초과할 수 없습니다.")
        String title,

        @CodePointLength(max = 30, message = "employmentType은 30자를 초과할 수 없습니다.")
        String employmentType,

        LocalDate deadline,

        @NotEmpty(message = "requirements는 1개 이상이어야 합니다.")
        @Size(max = 50, message = "requirements는 50개를 초과할 수 없습니다.")
        List<@NotNull(message = "requirements에 null을 넣을 수 없습니다.") @Valid Requirement> requirements
) {

    /**
     * 공고 원문 하나의 최대 길이. LLM 입력 비용과 시간을 묶어 두려고 둔다. 추출 요청도 같은 값을 쓴다.
     */
    public static final int MAX_CONTENT_LENGTH = 10_000;

    /**
     * 공백이 아닌 글자가 하나라도 있어야 한다. @NotBlank 는 전각 공백(U+3000) 같은 유니코드 공백을 글자로 봐서,
     * 그런 공백만 들어오면 도메인 검증에서 500 이 난다. 그래서 필수 문자열은 @NotBlank 대신 @NotNull 과 이 패턴으로
     * 유니코드 공백까지 공백으로 보고 400 으로 막는다.
     */
    public static final String HAS_TEXT = "(?U)(?s).*\\S.*";

    /**
     * 검증 전에 문자열의 앞뒤 공백을 자르고, 비어 있으면 null 로 둔다.
     * 공고 원문은 해시를 만들 때 공백을 따로 정리하므로 그대로 둔다.
     */
    public AdminJobPostingSaveRequest {
        sourceUrl = DataResolveUtil.sanitizeString(sourceUrl);
        companyName = DataResolveUtil.sanitizeString(companyName);
        careerRequirement = DataResolveUtil.sanitizeString(careerRequirement);
        title = DataResolveUtil.sanitizeString(title);
        employmentType = DataResolveUtil.sanitizeString(employmentType);
    }

    public record Requirement(
            @NotNull(message = "requirements의 name은 필수입니다.")
            @Pattern(regexp = HAS_TEXT, message = "requirements의 name은 필수입니다.")
            @CodePointLength(max = 100, message = "requirements의 name은 100자를 초과할 수 없습니다.")
            String name,

            @NotNull(message = "requirements의 category는 필수입니다.")
            RequirementCategory category,

            @NotNull(message = "requirements의 summary는 필수입니다.")
            @Pattern(regexp = HAS_TEXT, message = "requirements의 summary는 필수입니다.")
            @CodePointLength(max = 500, message = "requirements의 summary는 500자를 초과할 수 없습니다.")
            String summary
    ) {

        public Requirement {
            name = DataResolveUtil.sanitizeString(name);
            summary = DataResolveUtil.sanitizeString(summary);
        }
    }
}
