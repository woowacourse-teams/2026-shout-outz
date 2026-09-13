package com.shoutoutz.api.user.application;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import com.shoutoutz.api.user.domain.account.Handle;
import com.shoutoutz.api.user.domain.profile.ProfileDisplayName;
import com.shoutoutz.api.user.application.dto.UserSearchCursor;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** 검색 정렬 기준을 외부에 노출하지 않는 URL-safe 커서로 변환한다. */
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
            throw new IllegalStateException("검색 커서를 생성할 수 없습니다.", exception);
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
            throw new IllegalArgumentException("유효하지 않은 커서입니다.");
        }
    }

    private void validate(UserSearchCursor cursor) {
        if (cursor == null
                || cursor.relevanceRank() < MIN_RELEVANCE_RANK
                || cursor.relevanceRank() > MAX_RELEVANCE_RANK) {
            throw new IllegalArgumentException("유효하지 않은 관련도 순위입니다.");
        }
        new ProfileDisplayName(cursor.displayName());
        new Handle(cursor.handle());
    }
}
