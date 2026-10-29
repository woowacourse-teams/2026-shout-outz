package com.shoutoutz.api.magazine.application;

import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.magazine.application.dto.MagazineCursor;
import com.shoutoutz.api.magazine.domain.MagazineErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;

public final class MagazineCursorCodec {
    private MagazineCursorCodec() {
    }

    public static MagazineCursor decode(String cursor, String scope) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|", -1);
            if (parts.length != 3 || !parts[0].equals(scope)) {
                throw new IllegalArgumentException();
            }
            long id = Long.parseLong(parts[2]);
            if (id <= 0) {
                throw new IllegalArgumentException();
            }
            return new MagazineCursor(Instant.parse(parts[1]), id);
        } catch (IllegalArgumentException | DateTimeException exception) {
            throw new InvalidInputException(MagazineErrorCode.MAGAZINE_INVALID_CURSOR, exception);
        }
    }

    public static String encode(Instant timestamp, long id, String scope) {
        String payload = scope + "|" + timestamp + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }
}
