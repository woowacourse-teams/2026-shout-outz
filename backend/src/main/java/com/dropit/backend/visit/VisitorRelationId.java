package com.dropit.backend.visit;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class VisitorRelationId implements Serializable {

    @Column(name = "visitor_id")
    private String visitorId;

    @Column(name = "visited_on")
    private LocalDate visitedOn;

    protected VisitorRelationId() {
    }

    public VisitorRelationId(String visitorId, LocalDate visitedOn) {
        this.visitorId = visitorId;
        this.visitedOn = visitedOn;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof VisitorRelationId that)) return false;
        return Objects.equals(visitorId, that.visitorId) && Objects.equals(visitedOn, that.visitedOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(visitorId, visitedOn);
    }
}
