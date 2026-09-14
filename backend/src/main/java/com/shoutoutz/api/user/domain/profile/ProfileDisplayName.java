package com.shoutoutz.api.user.domain.profile;

import com.shoutoutz.api.common.util.DataResolveUtil;

public record ProfileDisplayName(String value) {

    public ProfileDisplayName {
        value = DataResolveUtil.sanitizeString(value);
        UserProfileValidator.validateDisplayName(value);
    }
}
