package com.dropit.backend.reaction;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AppRelationId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "app_id")
    private String appId;

    protected AppRelationId() {
    }

    public AppRelationId(UUID userId, String appId) {
        this.userId = userId;
        this.appId = appId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getAppId() {
        return appId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof AppRelationId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(appId, that.appId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, appId);
    }
}
