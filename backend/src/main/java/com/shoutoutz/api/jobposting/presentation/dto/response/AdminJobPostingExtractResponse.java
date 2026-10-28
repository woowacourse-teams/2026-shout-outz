package com.shoutoutz.api.jobposting.presentation.dto.response;

import com.shoutoutz.api.jobposting.application.dto.ExtractedJobPosting;
import com.shoutoutz.api.jobposting.application.dto.ExtractedJobRequirement;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import java.time.LocalDate;
import java.util.List;

/**
 * 운영자가 확인하고 고칠 추출 결과. 같은 주소로 이미 등록된 공고가 있으면 그 id 를 함께 알려 준다.
 */
public record AdminJobPostingExtractResponse(
        String companyName,
        JobRole jobRole,
        String title,
        String employmentType,
        String careerRequirement,
        LocalDate deadline,
        List<Requirement> requirements,
        Long registeredJobPostingId
) {

    public AdminJobPostingExtractResponse {
        requirements = List.copyOf(requirements);
    }

    public static AdminJobPostingExtractResponse from(ExtractedJobPosting extracted, Long registeredJobPostingId) {
        return new AdminJobPostingExtractResponse(
                extracted.companyName(),
                extracted.jobRole(),
                extracted.title(),
                extracted.employmentType(),
                extracted.careerRequirement(),
                extracted.deadline(),
                extracted.requirements().stream().map(Requirement::from).toList(),
                registeredJobPostingId
        );
    }

    public record Requirement(String name, RequirementCategory category, String summary) {

        private static Requirement from(ExtractedJobRequirement requirement) {
            return new Requirement(requirement.name(), requirement.category(), requirement.summary());
        }
    }
}
