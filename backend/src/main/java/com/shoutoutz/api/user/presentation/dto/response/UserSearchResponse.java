package com.shoutoutz.api.user.presentation.dto.response;

import com.shoutoutz.api.user.application.dto.UserSearchItem;
import com.shoutoutz.api.user.application.dto.UserSearchResult;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.user.domain.profile.Track;
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
                    trackValue(item.track()),
                    cohortValue(item.cohort()),
                    item.avatarImageId()
            );
        }

        private static String trackValue(Track track) {
            if (track == null) {
                return null;
            }
            return track.getValue();
        }

        private static Short cohortValue(Cohort cohort) {
            if (cohort == null) {
                return null;
            }
            return (short) cohort.getValue();
        }
    }

    public record Meta(
            String nextCursor,
            boolean hasNext
    ) {

        public static Meta from(UserSearchResult result) {
            return new Meta(result.nextCursor(), result.hasNext());
        }
    }
}
