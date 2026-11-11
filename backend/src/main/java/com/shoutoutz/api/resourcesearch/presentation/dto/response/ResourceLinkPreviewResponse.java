package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import com.shoutoutz.api.resourcesearch.application.dto.ResourceLinkPreview;

public record ResourceLinkPreviewResponse(String title, String description, String imageUrl, String siteName) {

    public static ResourceLinkPreviewResponse from(ResourceLinkPreview linkPreview) {
        if (linkPreview == null) {
            return null;
        }
        return new ResourceLinkPreviewResponse(
                linkPreview.title(),
                linkPreview.description(),
                linkPreview.imageUrl(),
                linkPreview.siteName()
        );
    }
}
