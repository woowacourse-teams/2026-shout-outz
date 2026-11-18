package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.LexicalMatch;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;

public interface LexicalChunkSearch {

    List<LexicalMatch> search(String query, ResourceTrack track, int limit);
}
