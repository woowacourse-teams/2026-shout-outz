package com.shoutoutz.api.jobposting.application;

import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_NOT_FOUND;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_REQUIREMENT_NOT_FOUND;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListCursor;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListItem;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListPage;
import com.shoutoutz.api.jobposting.application.dto.JobPostingSearchCondition;
import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.JobPostingRepository;
import com.shoutoutz.api.jobposting.domain.JobPostingSort;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.presentation.dto.request.JobPostingFindAllRequest;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingDetailResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingFindAllResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingListMetaResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobRequirementRecommendationResponse;
import com.shoutoutz.api.media.application.MediaUrlResolver;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    /**
     * 회사 로고는 목록과 상세 모두 작게 보여서 작은 이미지를 쓴다.
     */
    private static final MediaVariant LOGO_VARIANT = MediaVariant.THUMBNAIL;

    private final JobPostingRepository jobPostingRepository;
    private final JobPostingQueryRepository queryRepository;
    private final MediaUrlResolver mediaUrlResolver;
    private final Clock clock;

    /**
     * 직군 탭 개수는 직군 조건을 빼고 노출 조건과 검색어만 반영해 센다.
     */
    @Transactional(readOnly = true)
    public JobPostingFindAllResponse findAll(JobPostingFindAllRequest request) {
        LocalDate today = today();
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
        return JobPostingFindAllResponse.from(page.items(), logoUrlsOf(page.items()), meta);
    }

    /**
     * 사용자에게 보여 줄 수 없는 공고(분석 안 끝남, 숨김, 마감)는 없는 공고처럼 404 로 응답한다.
     */
    @Transactional(readOnly = true)
    public JobPostingDetailResponse findDetail(long jobPostingId) {
        JobPosting jobPosting = findVisible(jobPostingId);
        URI logoUrl = mediaUrlResolver.resolve(jobPosting.getLogoMediaId(), LOGO_VARIANT);
        return JobPostingDetailResponse.from(
                jobPosting,
                logoUrl == null ? null : logoUrl.toString(),
                queryRepository.countRecommendationsByRequirement(jobPostingId)
        );
    }

    /**
     * 공고를 볼 수 없으면 공고 404, 공고는 보이는데 그 공고의 역량이 아니면 역량 404 로 응답한다.
     */
    @Transactional(readOnly = true)
    public List<JobRequirementRecommendationResponse> findRecommendations(long jobPostingId, long requirementId) {
        JobPosting jobPosting = findVisible(jobPostingId);
        if (!jobPosting.hasRequirement(requirementId)) {
            throw new EntityNotFoundException(JOB_REQUIREMENT_NOT_FOUND);
        }
        return queryRepository.findRecommendations(requirementId).stream()
                .map(JobRequirementRecommendationResponse::from)
                .toList();
    }

    private JobPosting findVisible(long jobPostingId) {
        return jobPostingRepository.findById(jobPostingId)
                .filter(posting -> posting.isVisibleOn(today()))
                .orElseThrow(() -> new EntityNotFoundException(JOB_POSTING_NOT_FOUND));
    }

    /**
     * 페이지에 담긴 공고의 로고 주소를 한 번에 구한다.
     */
    private Map<Long, URI> logoUrlsOf(List<JobPostingListItem> items) {
        List<Long> logoMediaIds = items.stream()
                .map(JobPostingListItem::logoMediaId)
                .filter(Objects::nonNull)
                .toList();
        return mediaUrlResolver.resolveAll(logoMediaIds, LOGO_VARIANT);
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(DEADLINE_ZONE));
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
