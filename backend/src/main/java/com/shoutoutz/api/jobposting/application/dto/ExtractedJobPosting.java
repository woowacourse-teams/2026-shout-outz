package com.shoutoutz.api.jobposting.application.dto;

import com.shoutoutz.api.jobposting.domain.JobRole;
import java.time.LocalDate;
import java.util.List;

/**
 * 원문에서 뽑은 공고 정보와 역량 항목 초안. 공고에 없는 값은 null 이다.
 */
public record ExtractedJobPosting(
        String companyName,
        JobRole jobRole,
        String title,
        String employmentType,
        String careerRequirement,
        LocalDate deadline,
        List<ExtractedJobRequirement> requirements
) {
    public ExtractedJobPosting {
        requirements = List.copyOf(requirements);
    }
}
