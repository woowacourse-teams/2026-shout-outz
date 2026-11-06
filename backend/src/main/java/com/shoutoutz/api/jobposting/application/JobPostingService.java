package com.shoutoutz.api.jobposting.application;

import com.shoutoutz.api.jobposting.application.dto.JobPostingListCursor;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListItem;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListPage;
import com.shoutoutz.api.jobposting.application.dto.JobPostingSearchCondition;
import com.shoutoutz.api.jobposting.domain.JobPostingSort;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.presentation.dto.request.JobPostingFindAllRequest;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingFindAllResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingListMetaResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자에게 보여 줄 채용공고를 읽는다. 로그인 없이 볼 수 있다.
 */
@Service
@RequiredArgsConstructor
public class JobPostingService {

    /**
     * 마감일은 한국 날짜로 적혀 있어 오늘도 한국 날짜로 구한다. Clock 빈은 UTC 이므로 시간대를 명시한다.
     */
    private static final ZoneId DEADLINE_ZONE = ZoneId.of("Asia/Seoul");

    private final JobPostingQueryRepository queryRepository;
    private final Clock clock;

    /**
     * 직군 탭 개수는 직군 조건을 빼고 노출 조건과 검색어만 반영해 센다.
     */
    @Transactional(readOnly = true)
    public JobPostingFindAllResponse findAll(JobPostingFindAllRequest request) {
        LocalDate today = LocalDate.now(clock.withZone(DEADLINE_ZONE));
        JobPostingListCursor cursor = request.cursor() == null
                ? null
                : JobPostingListCursorCodec.decode(request.cursor(), request.resolvedSort());

        JobPostingListPage page = queryRepository.findAll(new JobPostingSearchCondition(
                request.jobRole(),
                request.keyword(),
                today,
                cursor,
                request.resolvedSize()
        ));
        Map<JobRole, Long> countsByJobRole = queryRepository.countByJobRole(request.keyword(), today);

        JobPostingListMetaResponse meta = JobPostingListMetaResponse.of(
                nextCursor(page, request.resolvedSort()),
                page.hasNext(),
                request.jobRole(),
                countsByJobRole
        );
        return JobPostingFindAllResponse.from(page.items(), meta);
    }

    /**
     * 정렬마다 커서에 담는 값이 달라 정렬별로 만든다. 정렬을 더하면 이 switch 가 컴파일 에러로 알려 준다.
     */
    private String nextCursor(JobPostingListPage page, JobPostingSort sort) {
        if (!page.hasNext()) {
            return null;
        }
        JobPostingListItem last = page.items().getLast();
        JobPostingListCursor cursor = switch (sort) {
            case LATEST -> JobPostingListCursor.latest(last.createdAt(), last.jobPostingId());
        };
        return JobPostingListCursorCodec.encode(cursor);
    }
}
