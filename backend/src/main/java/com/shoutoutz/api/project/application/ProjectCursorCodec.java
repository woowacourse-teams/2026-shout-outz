package com.shoutoutz.api.project.application;

import com.shoutoutz.api.project.domain.ProjectCursor;
import com.shoutoutz.api.project.domain.ProjectSort;
import com.shoutoutz.api.project.domain.exception.InvalidProjectCursorException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 프로젝트 목록 커서를 인코딩, 디코딩한다.
 * 클라이언트는 커서를 내부 구조를 모르는 문자열로 다루며, 받은 값을 다음 요청에 그대로 전달한다.
 * 쿼리스트링에 그대로 넣어도 깨지지 않도록 URL-safe Base64(패딩 없음)로 인코딩한다.
 *
 * 인코딩 전 형식
 * - LATEST : LATEST|{등록 시각}|{프로젝트 ID}
 * - POPULAR: POPULAR|{좋아요 수}|{등록 시각}|{프로젝트 ID}
 * - RELEVANCE: RELEVANCE|{등록 시각}|{프로젝트 ID}
 * - RELEVANCE 순위 목록: RELEVANCE|RANKED|{읽은 위치}|{프로젝트 ID,프로젝트 ID,...}|{검색어 지문}
 */
public final class ProjectCursorCodec {

    private static final String DELIMITER = "|";
    private static final String DELIMITER_REGEX = "\\|";
    private static final int LATEST_PARTS = 3;
    private static final int POPULAR_PARTS = 4;
    private static final int RANKED_PARTS = 5;
    private static final String RANKED_MARKER = "RANKED";
    private static final String ID_DELIMITER = ",";

    /**
     * 순위 목록 30개를 담은 커서도 1,000자 안쪽이다. 더 긴 값은 해독하기 전에 거른다.
     */
    private static final int MAX_ENCODED_LENGTH = 1024;

    private ProjectCursorCodec() {
    }

    public static String encode(ProjectCursor cursor) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload(cursor).getBytes(StandardCharsets.UTF_8));
    }

    private static String payload(ProjectCursor cursor) {
        if (cursor.isRanked()) {
            String ids = cursor.rankedIds().stream().map(String::valueOf).collect(Collectors.joining(ID_DELIMITER));
            return String.join(DELIMITER, cursor.sort().name(), RANKED_MARKER, String.valueOf(cursor.position()), ids,
                    cursor.keywordKey());
        }
        if (cursor.sort() == ProjectSort.POPULAR) {
            return String.join(DELIMITER, cursor.sort().name(), String.valueOf(cursor.likeCount()),
                    cursor.createdAt().toString(), String.valueOf(cursor.id()));
        }
        return String.join(DELIMITER, cursor.sort().name(), cursor.createdAt().toString(), String.valueOf(cursor.id()));
    }

    /**
     * 커서를 해독한다. 형식이 깨졌거나, 요청한 정렬 기준과 커서의 정렬 기준이 다르면 400 에러를 던진다.
     * 정렬을 바꾸면 이전 커서의 위치 기준이 맞지 않으므로, 커서 없이 처음부터 다시 조회해야 한다.
     */
    public static ProjectCursor decode(String encodedCursor, ProjectSort expectedSort) {
        if (encodedCursor.length() > MAX_ENCODED_LENGTH) {
            throw new InvalidProjectCursorException();
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(encodedCursor), StandardCharsets.UTF_8);
            String[] parts = payload.split(DELIMITER_REGEX, -1);
            ProjectSort sort = ProjectSort.valueOf(parts[0]);
            if (sort != expectedSort) {
                throw new InvalidProjectCursorException();
            }
            if (sort == ProjectSort.RELEVANCE && parts.length == RANKED_PARTS && parts[1].equals(RANKED_MARKER)) {
                List<Long> rankedIds = Arrays.stream(parts[3].split(ID_DELIMITER, -1)).map(Long::parseLong).toList();
                return ProjectCursor.ranked(rankedIds, Integer.parseInt(parts[2]), parts[4]);
            }
            if (sort == ProjectSort.POPULAR) {
                validatePartCount(parts, POPULAR_PARTS);
                return ProjectCursor.popular(Long.parseLong(parts[1]), Instant.parse(parts[2]), Long.parseLong(parts[3]));
            }
            validatePartCount(parts, LATEST_PARTS);
            return new ProjectCursor(sort, null, Instant.parse(parts[1]), Long.parseLong(parts[2]));
        } catch (RuntimeException exception) {
            throw new InvalidProjectCursorException();
        }
    }

    private static void validatePartCount(String[] parts, int expected) {
        if (parts.length != expected) {
            throw new InvalidProjectCursorException();
        }
    }
}
