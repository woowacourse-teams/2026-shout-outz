package com.shoutoutz.api.jobposting.application.dto;

import com.shoutoutz.api.jobposting.domain.JobRole;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 사용자 채용공고 목록의 한 줄.
 * requirementNames 는 역량 순서대로 앞 3개, requirementCount 는 전체 역량 수다. 마감일이 없으면 상시 채용이다.
 * createdAt 은 다음 페이지 커서를 만들 때 쓴다.
 */
public record JobPostingListItem(
        long jobPostingId,
        String companyName,
        JobRole jobRole,
        String employmentType,
        String careerRequirement,
        String title,
        LocalDate deadline,
        int requirementCount,
        List<String> requirementNames,
        Instant createdAt
) {

    public JobPostingListItem {
        requirementNames = List.copyOf(requirementNames);
    }
}
