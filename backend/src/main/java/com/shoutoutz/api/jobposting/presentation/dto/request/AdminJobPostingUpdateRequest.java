package com.shoutoutz.api.jobposting.presentation.dto.request;

import static com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingSaveRequest.HAS_TEXT;
import static com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingSaveRequest.MAX_CONTENT_LENGTH;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shoutoutz.api.common.util.DataResolveUtil;
import com.shoutoutz.api.jobposting.domain.JobPostingUrl;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingSaveRequest.Requirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.validator.constraints.CodePointLength;

/**
 * 운영자가 고친 공고 정보와 역량 목록. 등록 요청과 같되 원문(content)은 선택값이다.
 * 원문은 저장하지 않아 수정 화면이 미리 채울 수 없으므로, 공고 정보만 고칠 때는 원문 없이 보낸다.
 * 원문이 없거나 공백뿐이면 null 로 두고, 원문이 같을 때처럼 역량과 분석 상태를 그대로 둔다.
 * 역량은 원문이 바뀌었을 때만 새로 만들므로, 원문을 보낼 때만 1개 이상 받는다. 원문이 없으면 비워 보내도 되고, 보내도 쓰지 않는다.
 * logoImageId 는 전체 교체라서 비워 보내면 로고를 지운다.
 */
public record AdminJobPostingUpdateRequest(
        @NotBlank(message = "sourceUrl은 필수입니다.")
        @CodePointLength(max = JobPostingUrl.MAX_LENGTH, message = "sourceUrl은 1,024자를 초과할 수 없습니다.")
        @Pattern(regexp = JobPostingUrl.REGEX, message = "sourceUrl은 http 또는 https URL 형식이어야 합니다.")
        String sourceUrl,

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

        Long logoImageId,

        @Size(max = 50, message = "requirements는 50개를 초과할 수 없습니다.")
        List<@NotNull(message = "requirements에 null을 넣을 수 없습니다.") @Valid Requirement> requirements
) {

    private static final java.util.regex.Pattern TEXT = java.util.regex.Pattern.compile(HAS_TEXT);

    /**
     * 검증 전에 문자열의 앞뒤 공백을 자르고, 비어 있으면 null 로 둔다.
     * 공고 원문은 해시를 만들 때 공백을 따로 정리하므로 자르지 않고, 유니코드 공백만 있으면 보내지 않은 것으로 본다.
     */
    public AdminJobPostingUpdateRequest {
        sourceUrl = DataResolveUtil.sanitizeString(sourceUrl);
        companyName = DataResolveUtil.sanitizeString(companyName);
        careerRequirement = DataResolveUtil.sanitizeString(careerRequirement);
        title = DataResolveUtil.sanitizeString(title);
        employmentType = DataResolveUtil.sanitizeString(employmentType);
        content = content != null && TEXT.matcher(content).matches() ? content : null;
        requirements = requirements == null ? List.of() : requirements;
    }

    /**
     * 원문을 보내면 원문이 바뀌었을 때 역량을 새로 만들어야 하므로 역량이 1개 이상 있어야 한다.
     * 오류 응답의 field 는 requirementsPresentWithContent 다.
     */
    @JsonIgnore
    @AssertTrue(message = "content를 보내면 requirements는 1개 이상이어야 합니다.")
    public boolean isRequirementsPresentWithContent() {
        return content == null || !requirements.isEmpty();
    }
}
