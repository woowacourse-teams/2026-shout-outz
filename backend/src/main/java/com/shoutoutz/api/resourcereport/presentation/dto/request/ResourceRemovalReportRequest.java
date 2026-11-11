package com.shoutoutz.api.resourcereport.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.List;
import org.hibernate.validator.constraints.CodePointLength;

/**
 * 필요 없는 자료 제보 요청. 고른 자료 모두에 같은 이유가 붙는다.
 */
public record ResourceRemovalReportRequest(
        @NotNull(message = "resourceIds는 필수입니다.")
        @Size(min = 1, max = 100, message = "resourceIds는 1개 이상 100개 이하여야 합니다.")
        List<@NotNull(message = "resourceIds에 빈 값을 넣을 수 없습니다.")
             @Positive(message = "resourceIds는 1 이상이어야 합니다.") Long> resourceIds,

        @CodePointLength(max = 500, message = "memo는 500자를 초과할 수 없습니다.")
        String memo
) {

    public ResourceRemovalReportRequest {
        memo = blankToNull(memo);
    }

    /**
     * 같은 자료를 여러 번 보내도 한 번만 제보하도록, 처음 나온 순서대로 중복을 뺀다.
     */
    public List<Long> distinctResourceIds() {
        return List.copyOf(new LinkedHashSet<>(resourceIds));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
