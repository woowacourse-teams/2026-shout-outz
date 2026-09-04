package com.shoutoutz.api.media.application.exception;

import com.shoutoutz.api.media.domain.MediaStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 아직 조회할 수 있는 이미지 변형본이 준비되지 않았을 때 발생한다.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class MediaNotReadyException extends RuntimeException {

    public MediaNotReadyException(long mediaId, MediaStatus status) {
        super("미디어가 아직 조회 가능한 상태가 아닙니다: " + mediaId + " (" + status + ")");
    }
}
