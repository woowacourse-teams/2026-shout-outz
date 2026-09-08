package com.shoutoutz.api.user.presentation.dto.response;

import com.shoutoutz.api.user.application.dto.result.UserSearchResult;
import com.shoutoutz.api.user.application.query.UserSearchItem;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.util.List;

public record UserSearchResponse(
        List<Item> items
) {

    public static UserSearchResponse from(UserSearchResult result) {
        List<Item> items = result.items().stream()
                .map(Item::from)
                .toList();
        return new UserSearchResponse(items);
    }

    public record Item(
            String handle,
            String displayName,
            UserType userType,
            String track,
            Short cohort,
            Long avatarImageId
    ) {

        private static Item from(UserSearchItem item) {
            return new Item(
                    item.handle(),
                    item.displayName(),
                    item.userType(),
                    item.track(),
                    item.cohort(),
                    item.avatarImageId()
            );
        }
    }
}
