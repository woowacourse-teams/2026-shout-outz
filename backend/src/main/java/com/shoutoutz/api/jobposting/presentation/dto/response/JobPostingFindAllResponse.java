package com.shoutoutz.api.jobposting.presentation.dto.response;

import com.shoutoutz.api.jobposting.application.dto.JobPostingListItem;
import com.shoutoutz.api.jobposting.domain.JobRole;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record JobPostingFindAllResponse(
        List<Item> items,
        JobPostingListMetaResponse meta
) {

    public JobPostingFindAllResponse {
        items = List.copyOf(items);
    }

    /**
     * logoUrls 는 로고 이미지 ID 별 주소다. 로고가 없거나 이미지가 준비되지 않은 공고는 companyLogoUrl 이 null 이다.
     */
    public static JobPostingFindAllResponse from(
            List<JobPostingListItem> listItems,
            Map<Long, URI> logoUrls,
            JobPostingListMetaResponse meta
    ) {
        return new JobPostingFindAllResponse(
                listItems.stream()
                        .map(listItem -> Item.from(listItem, logoUrlOf(listItem, logoUrls)))
                        .toList(),
                meta
        );
    }

    /**
     * 받은 주소 맵은 불변 맵이라 null 키로 찾으면 예외가 나서, 로고가 없는 공고는 먼저 걸러 낸다.
     */
    private static URI logoUrlOf(JobPostingListItem listItem, Map<Long, URI> logoUrls) {
        if (listItem.logoMediaId() == null) {
            return null;
        }
        return logoUrls.get(listItem.logoMediaId());
    }

    /**
     * 목록 한 줄. 마감일이 없으면 상시 채용이라 deadline 은 null 이다. 회사 로고가 없으면 companyLogoUrl 은 null 이다.
     */
    public record Item(
            long jobPostingId,
            String companyName,
            String companyLogoUrl,
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

        private static Item from(JobPostingListItem listItem, URI logoUrl) {
            return new Item(
                    listItem.jobPostingId(),
                    listItem.companyName(),
                    logoUrl == null ? null : logoUrl.toString(),
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
