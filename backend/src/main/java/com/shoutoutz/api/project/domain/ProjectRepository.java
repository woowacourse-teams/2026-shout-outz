package com.shoutoutz.api.project.domain;

import java.util.List;

public interface ProjectRepository {

    Project save(Project project, List<Long> techTagIds, List<Long> memberIds);

    boolean existsBySlug(Slug slug);
}
