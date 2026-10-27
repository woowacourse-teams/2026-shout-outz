package com.shoutoutz.api.jobposting.application;

import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_ADMIN_FORBIDDEN;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_DUPLICATE_URL;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_NOT_FOUND;

import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.jobposting.application.dto.ExtractedJobPosting;
import com.shoutoutz.api.jobposting.application.dto.JobPostingCursor;
import com.shoutoutz.api.jobposting.application.dto.JobPostingPage;
import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.JobPostingRepository;
import com.shoutoutz.api.jobposting.domain.JobPostingUrl;
import com.shoutoutz.api.jobposting.domain.JobRequirement;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingExtractRequest;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingFindAllRequest;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingSaveRequest;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingDetailResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingExtractResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingFindAllResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingSaveResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Clock;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobPostingAdminService {

    private final JobPostingRepository jobPostingRepository;
    private final JobPostingAdminQueryRepository queryRepository;
    private final JobPostingExtractor extractor;
    private final JobPostingCursorCodec cursorCodec;
    private final Clock clock;

    /**
     * 아무것도 저장하지 않는다. LLM 을 기다리는 동안 DB 연결을 붙잡지 않도록 트랜잭션을 걸지 않는다.
     * 같은 주소로 등록된 공고가 있어도 막지 않고 id 만 알려 준다. 막는 건 등록할 때 한다.
     */
    public AdminJobPostingExtractResponse extract(UserRole role, AdminJobPostingExtractRequest request) {
        validateAdmin(role);
        Long registeredJobPostingId = jobPostingRepository.findIdBySourceUrl(new JobPostingUrl(request.sourceUrl()))
                .orElse(null);
        ExtractedJobPosting extracted = extractor.extract(request.content());
        return AdminJobPostingExtractResponse.from(extracted, registeredJobPostingId);
    }

    /**
     * 같은 주소의 공고가 있으면 409 로 막는다. 사전 검사를 동시에 통과한 요청은 저장소가 DB 제약 위반을 409 로 바꾼다.
     */
    @Transactional
    public AdminJobPostingSaveResponse register(
            long adminUserId,
            UserRole role,
            AdminJobPostingSaveRequest request
    ) {
        validateAdmin(role);
        JobPosting jobPosting = JobPosting.register(
                request.sourceUrl(),
                request.content(),
                request.companyName(),
                request.jobRole(),
                request.careerRequirement(),
                request.title(),
                request.employmentType(),
                request.deadline(),
                adminUserId,
                toRequirements(request.requirements()),
                clock.instant()
        );
        if (jobPostingRepository.findIdBySourceUrl(jobPosting.getSourceUrl()).isPresent()) {
            throw new DuplicateEntityException(JOB_POSTING_DUPLICATE_URL);
        }
        return AdminJobPostingSaveResponse.from(jobPostingRepository.save(jobPosting));
    }

    @Transactional(readOnly = true)
    public AdminJobPostingFindAllResponse findAll(UserRole role, AdminJobPostingFindAllRequest request) {
        validateAdmin(role);
        JobPostingPage page = queryRepository.findAll(
                cursorCodec.decode(request.cursor()),
                request.resolvedSize()
        );
        String nextCursor = page.hasNext()
                ? cursorCodec.encode(new JobPostingCursor(page.items().getLast().id()))
                : null;
        return AdminJobPostingFindAllResponse.from(
                page.items(),
                new SliceMetaResponse(nextCursor, page.hasNext(), page.totalCount())
        );
    }

    @Transactional(readOnly = true)
    public AdminJobPostingDetailResponse findDetail(long jobPostingId, UserRole role) {
        validateAdmin(role);
        JobPosting jobPosting = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new EntityNotFoundException(JOB_POSTING_NOT_FOUND));
        return AdminJobPostingDetailResponse.from(jobPosting);
    }

    /**
     * 운영자가 보낸 순서대로 1부터 번호를 붙인다.
     */
    private List<JobRequirement> toRequirements(List<AdminJobPostingSaveRequest.Requirement> requirements) {
        return IntStream.range(0, requirements.size())
                .mapToObj(index -> {
                    AdminJobPostingSaveRequest.Requirement requirement = requirements.get(index);
                    return JobRequirement.create(
                            index + 1,
                            requirement.name(),
                            requirement.category(),
                            requirement.summary()
                    );
                })
                .toList();
    }

    private void validateAdmin(UserRole role) {
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException(JOB_POSTING_ADMIN_FORBIDDEN);
        }
    }
}
