package com.shoutoutz.api.project.domain;

public interface ProjectReactionRepository {

    void add(long projectId, long userId, ProjectReactionType type);

    void remove(long projectId, long userId, ProjectReactionType type);

    ProjectReactionCounts countByProjectId(long projectId);
}
