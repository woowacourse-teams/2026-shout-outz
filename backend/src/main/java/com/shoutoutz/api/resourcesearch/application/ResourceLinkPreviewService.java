package com.shoutoutz.api.resourcesearch.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceLinkPreviewService {

    static final int BATCH_SIZE = 20;

    private final ResourceLinkPreviewRepository resourceLinkPreviewRepository;

    public int linkUnlinkedResources() {
        return resourceLinkPreviewRepository.linkUnlinkedResources(BATCH_SIZE);
    }
}
