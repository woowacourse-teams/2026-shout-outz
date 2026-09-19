package com.shoutoutz.api.user.presentation.dto.response;

import com.shoutoutz.api.user.application.dto.UserSearchItem;
import com.shoutoutz.api.user.application.dto.UserSearchResult;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.user.domain.profile.Track;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.net.URI;
import java.util.List;
import java.util.Map;

public record UserSearchResponse(
        List<Item> items
) {

    public static UserSearchResponse from(UserSearchResult result) {
        List<Item> items = result.items().stream()
                .map(item -> Item.from(item, result.avatarUrls()))
                .toList();
        return new UserSearchResponse(items);
    }

    public record Item(
            String handle,
            String displayName,
            UserType userType,
            String track,
            Short cohort,
            String avatarUrl
    ) {

        private static Item from(UserSearchItem item, Map<Long, URI> avatarUrls) {
            return new Item(
                    item.handle(),
                    item.displayName(),
                    item.userType(),
                    trackValue(item.track()),
                    cohortValue(item.cohort()),
                    toUrl(findUrl(avatarUrls, item.avatarImageId()))
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

        private static String toUrl(URI url) {
            return url == null ? null : url.toString();
        }

        private static URI findUrl(Map<Long, URI> avatarUrls, Long avatarImageId) {
            return avatarImageId == null ? null : avatarUrls.get(avatarImageId);
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
