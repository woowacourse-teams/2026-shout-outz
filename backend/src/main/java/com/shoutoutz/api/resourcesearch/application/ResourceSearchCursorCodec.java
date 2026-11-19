package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.InternalServerErrorException;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCursor;
import com.shoutoutz.api.resourcesearch.domain.ResourceSearchErrorCode;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 정렬 기준을 외부에 노출하지 않는 URL-safe 커서 변환기
 */
@Component
@RequiredArgsConstructor
public class ResourceSearchCursorCodec {

    private final ObjectMapper objectMapper;

    public String encode(ResourceSearchCursor cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(cursor));
        } catch (JacksonException exception) {
            throw new InternalServerErrorException(
                    ResourceSearchErrorCode.RESOURCE_SEARCH_CURSOR_ENCODING_FAILED,
                    exception
            );
        }
    }

    public ResourceSearchCursor decode(String encodedCursor) {
        if (encodedCursor == null) {
            return null;
        }
        try {
            ResourceSearchCursor cursor = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(encodedCursor),
                    ResourceSearchCursor.class
            );
            if (cursor == null) {
                throw new BadRequestException(ResourceSearchErrorCode.RESOURCE_SEARCH_CURSOR_INVALID);
            }
            return cursor;
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new BadRequestException(ResourceSearchErrorCode.RESOURCE_SEARCH_CURSOR_INVALID, exception);
        }
    }
}
