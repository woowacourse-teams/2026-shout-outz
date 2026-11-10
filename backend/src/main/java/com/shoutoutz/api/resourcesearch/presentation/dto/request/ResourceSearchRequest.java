package com.shoutoutz.api.resourcesearch.presentation.dto.request;

import com.shoutoutz.api.common.util.DataResolveUtil;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import com.shoutoutz.api.resourcesearch.domain.SearchKeyword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.CodePointLength;

public record ResourceSearchRequest(
        @NotBlank(message = "keyword는 비어 있을 수 없습니다.")
        @CodePointLength(max = SearchKeyword.MAX_LENGTH, message = "keyword는 50자 이하여야 합니다.")
        String keyword,

        ResourceTrack track
) {

    public ResourceSearchRequest {
        keyword = DataResolveUtil.sanitizeString(keyword);
    }

    @AssertTrue(message = "track은 BE, FE, AN 중 하나여야 합니다.")
    public boolean isTrackSupported() {
        return track != ResourceTrack.ALL;
    }
}
