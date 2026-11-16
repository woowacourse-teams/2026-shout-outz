package com.shoutoutz.api.resourcesearch.presentation.dto.request;

import com.shoutoutz.api.common.util.DataResolveUtil;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import com.shoutoutz.api.resourcesearch.domain.SearchKeyword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.CodePointLength;

public record ResourceSearchMoreRequest(
        @NotBlank(message = "keyword는 비어 있을 수 없습니다.")
        @CodePointLength(max = SearchKeyword.MAX_LENGTH, message = "keyword는 50자 이하여야 합니다.")
        String keyword,

        ResourceTrack track,

        @NotNull(message = "section은 BLOG 또는 DOCUMENT여야 합니다.")
        ResourceSection section,

        @Size(max = 1000, message = "cursor는 1000자 이하여야 합니다.")
        String cursor
) {

    public ResourceSearchMoreRequest {
        keyword = DataResolveUtil.sanitizeString(keyword);
    }

    @AssertTrue(message = "track은 BE, FE, AN 중 하나여야 합니다.")
    public boolean isTrackSupported() {
        return track != ResourceTrack.ALL;
    }

    @AssertTrue(message = "section은 BLOG 또는 DOCUMENT여야 합니다.")
    public boolean isSectionSupported() {
        return section != ResourceSection.TALK;
    }
}
