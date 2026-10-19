package com.shoutoutz.api.magazine.application.dto;

import java.time.Instant;

public record MagazineCursor(
        Instant timestamp,
        long id
) {
}
