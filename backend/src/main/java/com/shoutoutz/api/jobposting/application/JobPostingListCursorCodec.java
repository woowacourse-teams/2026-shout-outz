package com.shoutoutz.api.jobposting.application;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListCursor;
import com.shoutoutz.api.jobposting.domain.JobPostingErrorCode;
import com.shoutoutz.api.jobposting.domain.JobPostingSort;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * 사용자 채용공고 목록 커서를 인코딩, 디코딩한다.
 * 클라이언트는 커서를 내부 구조를 모르는 문자열로 다루며, 받은 값을 다음 요청에 그대로 전달한다.
 * 쿼리스트링에 그대로 넣어도 깨지지 않도록 URL-safe Base64(패딩 없음)로 인코딩한다.
 *
 * 인코딩 전 형식
 * - LATEST: LATEST|{등록 시각}|{채용공고 ID}
 */
public final class JobPostingListCursorCodec {

    private static final String DELIMITER = "|";
    private static final String DELIMITER_REGEX = "\\|";
    private static final int LATEST_PARTS = 3;

    private JobPostingListCursorCodec() {
    }

    public static String encode(JobPostingListCursor cursor) {
        String payload = String.join(DELIMITER,
                cursor.sort().name(), cursor.createdAt().toString(), String.valueOf(cursor.jobPostingId()));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 커서를 해독한다. 형식이 깨졌거나, 요청한 정렬 기준과 커서의 정렬 기준이 다르면 400 에러를 던진다.
     * 정렬을 바꾸면 이전 커서의 위치 기준이 맞지 않으므로, 커서 없이 처음부터 다시 조회해야 한다.
     */
    public static JobPostingListCursor decode(String encodedCursor, JobPostingSort expectedSort) {
        try {
            String payload = new String(Base64.getUrlDecoder().decode(encodedCursor), StandardCharsets.UTF_8);
            String[] parts = payload.split(DELIMITER_REGEX, -1);
            JobPostingSort sort = JobPostingSort.valueOf(parts[0]);
            if (sort != expectedSort || parts.length != LATEST_PARTS) {
                throw new IllegalArgumentException();
            }
            return JobPostingListCursor.latest(Instant.parse(parts[1]), Long.parseLong(parts[2]));
        } catch (RuntimeException exception) {
            throw new BadRequestException(JobPostingErrorCode.JOB_POSTING_CURSOR_INVALID, exception);
        }
    }
}
