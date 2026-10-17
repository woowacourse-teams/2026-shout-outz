package com.shoutoutz.api.magazine.application.dto;

import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.magazine.presentation.dto.response.MagazineSummaryResponse;
import java.util.List;

public record MagazinePage(
        List<MagazineSummaryResponse> items,
        SliceMetaResponse meta
) {
}
