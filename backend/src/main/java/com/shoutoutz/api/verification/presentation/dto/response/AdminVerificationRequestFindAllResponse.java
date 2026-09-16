package com.shoutoutz.api.verification.presentation.dto.response;

import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.verification.application.dto.AdminVerificationRequestItem;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import java.time.Instant;
import java.util.List;

public record AdminVerificationRequestFindAllResponse(
        List<Item> items,
        String nextCursor
) {

    public AdminVerificationRequestFindAllResponse {
        items = List.copyOf(items);
    }

    public static AdminVerificationRequestFindAllResponse from(
            List<AdminVerificationRequestItem> items,
            String nextCursor
    ) {
        return new AdminVerificationRequestFindAllResponse(
                items.stream().map(Item::from).toList(),
                nextCursor
        );
    }

    public record Item(
            long requestId,
            Applicant applicant,
            UserType userType,
            String nickname,
            Integer cohort,
            String track,
            VerificationRequestStatus status,
            Instant requestedAt
    ) {

        private static Item from(AdminVerificationRequestItem item) {
            return new Item(
                    item.requestId(),
                    new Applicant(item.userId(), item.handle()),
                    item.userType(),
                    item.nickname(),
                    item.cohort(),
                    item.track(),
                    item.status(),
                    item.requestedAt()
            );
        }
    }

    public record Applicant(long userId, String handle) {
    }
}
