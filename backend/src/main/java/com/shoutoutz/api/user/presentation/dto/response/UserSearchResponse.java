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
                .map(item -> Item.from(item, result.avatarUrls(), result.userAvatarUrls()))
                .toList();
        return new UserSearchResponse(items);
    }

    public record Item(
            Long userId,
            String handle,
            String displayName,
            UserType userType,
            String track,
            Short cohort,
            Long avatarImageId,
            String avatarUrl
    ) {

        public Item(
                String handle,
                String displayName,
                UserType userType,
                String track,
                Short cohort,
                Long avatarImageId,
                String avatarUrl
        ) {
            this(null, handle, displayName, userType, track, cohort, avatarImageId, avatarUrl);
        }

        private static Item from(
                UserSearchItem item,
                Map<Long, URI> avatarUrls,
                Map<Long, String> userAvatarUrls
        ) {
            return new Item(
                    item.userId(),
                    item.handle(),
                    item.displayName(),
                    item.userType(),
                    trackValue(item.userType(), item.track()),
                    cohortValue(item.userType(), item.cohort()),
                    item.avatarImageId(),
                    avatarUrl(item, avatarUrls, userAvatarUrls)
            );
        }

        private static String avatarUrl(
                UserSearchItem item,
                Map<Long, URI> avatarUrls,
                Map<Long, String> userAvatarUrls
        ) {
            URI mediaUrl = findUrl(avatarUrls, item.avatarImageId());
            if (mediaUrl != null) {
                return mediaUrl.toString();
            }
            return item.userId() == null ? null : userAvatarUrls.get(item.userId());
        }

        private static String trackValue(UserType userType, Track track) {
            if (userType != UserType.WOOWACOURSE_CREW || track == null) {
                return null;
            }
            return track.getValue();
        }

        private static Short cohortValue(UserType userType, Cohort cohort) {
            if (userType != UserType.WOOWACOURSE_CREW || cohort == null) {
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
}
