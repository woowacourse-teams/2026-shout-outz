package com.shoutoutz.api.resourcereport.presentation.dto.response;

import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.resourcereport.application.dto.ResourceReportAdminItem;
import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import java.time.Instant;
import java.util.List;

public record ResourceReportAdminFindAllResponse(
        List<Item> items,
        SliceMetaResponse meta
) {

    public ResourceReportAdminFindAllResponse {
        items = List.copyOf(items);
    }

    public static ResourceReportAdminFindAllResponse from(
            List<ResourceReportAdminItem> reports,
            SliceMetaResponse meta
    ) {
        return new ResourceReportAdminFindAllResponse(
                reports.stream().map(Item::from).toList(),
                meta
        );
    }

    /**
     * @param resource 필요 없어요 제보가 가리키는 자료. 추가해 주세요 제보이거나 자료가 내려갔으면 null
     * @param url      추가해 주세요 제보로 알려 준 주소. 필요 없어요 제보이면 null
     */
    public record Item(
            long reportId,
            ResourceReportKind kind,
            Project project,
            Resource resource,
            String url,
            String memo,
            long reporterUserId,
            ResourceReportStatus status,
            Instant createdAt
    ) {

        private static Item from(ResourceReportAdminItem report) {
            return new Item(
                    report.reportId(),
                    report.kind(),
                    new Project(report.projectSlug(), report.projectTitle()),
                    report.resourceId() == null
                            ? null
                            : new Resource(
                                    report.resourceId(),
                                    report.resourceSource(),
                                    report.resourceTitle(),
                                    report.resourceUrl()
                            ),
                    report.url(),
                    report.memo(),
                    report.reporterUserId(),
                    report.status(),
                    report.createdAt()
            );
        }
    }

    public record Project(String slug, String title) {
    }

    public record Resource(long resourceId, String source, String title, String url) {
    }
}
