package com.shoutoutz.api.user.domain.profile;

import com.shoutoutz.api.common.exception.custom.BadRequestException;

public class InvalidTrackException extends BadRequestException {

    public InvalidTrackException() {
        super(UserProfileErrorCode.INVALID_TRACK);
    }
}
