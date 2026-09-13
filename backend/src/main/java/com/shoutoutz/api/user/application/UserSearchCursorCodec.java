package com.shoutoutz.api.user.application;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import com.shoutoutz.api.common.exception.custom.InternalServerErrorException;
import com.shoutoutz.api.user.application.dto.UserSearchCursor;
import com.shoutoutz.api.user.domain.account.Handle;
import com.shoutoutz.api.user.domain.account.UserErrorCode;
import com.shoutoutz.api.user.domain.profile.ProfileDisplayName;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 검색 정렬 기준을 외부에 노출하지 않는 URL-safe 커서 변환기.
 */
@Component
@RequiredArgsConstructor
public class UserSearchCursorCodec {

    private static final int MIN_RELEVANCE_RANK = 0;
    private static final int MAX_RELEVANCE_RANK = 2;

    private final ObjectMapper objectMapper;

    String encode(UserSearchCursor cursor) {
        try {
            byte[] serializedCursor = objectMapper.writeValueAsBytes(cursor);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(serializedCursor);
        } catch (JacksonException exception) {
            throw new InternalServerErrorException(
                    UserErrorCode.USER_SEARCH_CURSOR_ENCODING_FAILED,
                    exception
            );
        }
    }

    UserSearchCursor decode(String encodedCursor) {
        if (encodedCursor == null) {
            return null;
        }

        try {
            byte[] decodedCursor = Base64.getUrlDecoder().decode(encodedCursor);
            UserSearchCursor cursor = objectMapper.readValue(decodedCursor, UserSearchCursor.class);
            validate(cursor);
            return cursor;
        } catch (JacksonException | IllegalArgumentException | DomainValidationException exception) {
            throw new BadRequestException(UserErrorCode.USER_SEARCH_CURSOR_INVALID, exception);
        }
    }

    private void validate(UserSearchCursor cursor) {
        if (cursor == null
                || cursor.relevanceRank() < MIN_RELEVANCE_RANK
                || cursor.relevanceRank() > MAX_RELEVANCE_RANK) {
            throw new BadRequestException(UserErrorCode.USER_SEARCH_CURSOR_INVALID);
        }
        new ProfileDisplayName(cursor.displayName());
        new Handle(cursor.handle());
    }
}
