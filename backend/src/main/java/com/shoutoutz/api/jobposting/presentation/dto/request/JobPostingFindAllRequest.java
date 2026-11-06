package com.shoutoutz.api.jobposting.presentation.dto.request;

import com.shoutoutz.api.common.util.DataResolveUtil;
import com.shoutoutz.api.jobposting.domain.JobPostingSort;
import com.shoutoutz.api.jobposting.domain.JobRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.hibernate.validator.constraints.CodePointLength;

/**
 * 사용자 채용공고 목록 조회 파라미터. 직군과 정렬이 없으면 전체 직군, 최신순으로 읽는다.
 */
public record JobPostingFindAllRequest(
        JobRole jobRole,

        @CodePointLength(max = 100, message = "keyword는 100자 이하여야 합니다.")
        String keyword,

        JobPostingSort sort,

        String cursor,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 50, message = "size는 50 이하여야 합니다.")
        Integer size
) {

    private static final int DEFAULT_SIZE = 20;

    public JobPostingFindAllRequest {
        keyword = DataResolveUtil.sanitizeString(keyword);
        cursor = DataResolveUtil.sanitizeString(cursor);
    }

    public JobPostingSort resolvedSort() {
        return sort == null ? JobPostingSort.LATEST : sort;
    }

    public int resolvedSize() {
        return size == null ? DEFAULT_SIZE : size;
    }
}
