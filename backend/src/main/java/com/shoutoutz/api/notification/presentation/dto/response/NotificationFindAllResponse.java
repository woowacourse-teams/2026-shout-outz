package com.shoutoutz.api.notification.presentation.dto.response;

import com.shoutoutz.api.common.response.SliceMetaResponse;
import java.util.List;

public record NotificationFindAllResponse(
        List<NotificationResponse> items,
        SliceMetaResponse meta
) {
}
