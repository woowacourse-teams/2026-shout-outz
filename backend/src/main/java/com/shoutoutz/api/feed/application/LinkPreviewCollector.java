package com.shoutoutz.api.feed.application;

import com.shoutoutz.api.feed.application.dto.LinkPreviewMetadata;
import java.io.IOException;

public interface LinkPreviewCollector {

    LinkPreviewMetadata collect(String url) throws IOException;
}
