package com.dropit.backend.crew;

import java.time.Instant;

public record CrewStatusResponse(boolean isCrewMember, Instant verifiedAt) {
}
