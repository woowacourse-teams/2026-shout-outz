package com.shoutoutz.api.jobposting.presentation.dto.response;

import com.shoutoutz.api.jobposting.application.dto.JobPostingListItem;
import com.shoutoutz.api.jobposting.domain.JobRole;
import java.time.LocalDate;
import java.util.List;

public record JobPostingFindAllResponse(
        List<Item> items,
        JobPostingListMetaResponse meta
) {

    public JobPostingFindAllResponse {
        items = List.copyOf(items);
    }

    public static JobPostingFindAllResponse from(List<JobPostingListItem> listItems, JobPostingListMetaResponse meta) {
        return new JobPostingFindAllResponse(
                listItems.stream().map(Item::from).toList(),
                meta
        );
    }

    /**
     * 목록 한 줄. 마감일이 없으면 상시 채용이라 deadline 은 null 이다.
     */
    public record Item(
            long jobPostingId,
            String companyName,
            JobRole jobRole,
            String employmentType,
            String careerRequirement,
            String title,
            LocalDate deadline,
            int requirementCount,
            List<String> requirementNames
    ) {

        public Item {
            requirementNames = List.copyOf(requirementNames);
        }

        private static Item from(JobPostingListItem listItem) {
            return new Item(
                    listItem.jobPostingId(),
                    listItem.companyName(),
                    listItem.jobRole(),
                    listItem.employmentType(),
                    listItem.careerRequirement(),
                    listItem.title(),
                    listItem.deadline(),
                    listItem.requirementCount(),
                    listItem.requirementNames()
            );
        }
    }
}
