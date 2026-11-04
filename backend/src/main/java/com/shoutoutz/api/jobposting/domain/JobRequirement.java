package com.shoutoutz.api.jobposting.domain;

import com.shoutoutz.api.common.util.DataResolveUtil;
import lombok.Getter;

/**
 * 채용공고에서 뽑은 역량 항목 하나
 * 공고 안에서 보여 줄 순서(displayOrder)를 갖고, 공고와 함께 저장된다.
 */
@Getter
public final class JobRequirement {

    private final Long id;
    private final int displayOrder;
    private final String name;
    private final RequirementCategory category;
    private final String summary;
    private final MatchingStatus status;

    private JobRequirement(
            Long id,
            int displayOrder,
            String name,
            RequirementCategory category,
            String summary,
            MatchingStatus status
    ) {
        String sanitizedName = DataResolveUtil.sanitizeString(name);
        String sanitizedSummary = DataResolveUtil.sanitizeString(summary);

        JobPostingValidator.validateJobRequirement(id, displayOrder, sanitizedName, category, sanitizedSummary, status);

        this.id = id;
        this.displayOrder = displayOrder;
        this.name = sanitizedName;
        this.category = category;
        this.summary = sanitizedSummary;
        this.status = status;
    }

    /**
     * 새 역량 항목은 매칭 전이므로 PENDING 으로 만든다.
     */
    public static JobRequirement create(
            int displayOrder,
            String name,
            RequirementCategory category,
            String summary
    ) {
        return new JobRequirement(null, displayOrder, name, category, summary, MatchingStatus.PENDING);
    }

    public static JobRequirement reconstitute(
            long id,
            int displayOrder,
            String name,
            RequirementCategory category,
            String summary,
            MatchingStatus status
    ) {
        return new JobRequirement(id, displayOrder, name, category, summary, status);
    }
}
