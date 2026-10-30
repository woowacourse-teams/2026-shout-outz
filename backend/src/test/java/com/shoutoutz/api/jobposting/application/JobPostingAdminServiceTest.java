package com.shoutoutz.api.jobposting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.jobposting.application.dto.ExtractedJobPosting;
import com.shoutoutz.api.jobposting.application.dto.ExtractedJobRequirement;
import com.shoutoutz.api.jobposting.application.dto.JobPostingCursor;
import com.shoutoutz.api.jobposting.application.dto.JobPostingPage;
import com.shoutoutz.api.jobposting.application.dto.JobPostingSummary;
import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.JobPostingContentHash;
import com.shoutoutz.api.jobposting.domain.JobPostingErrorCode;
import com.shoutoutz.api.jobposting.domain.JobPostingRepository;
import com.shoutoutz.api.jobposting.domain.JobPostingUrl;
import com.shoutoutz.api.jobposting.domain.JobPostingVisibility;
import com.shoutoutz.api.jobposting.domain.JobRequirement;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingStatus;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingExtractRequest;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingFindAllRequest;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingSaveRequest;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingVisibilityUpdateRequest;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingUpdateResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingVisibilityUpdateResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobPostingAdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T00:00:00Z");
    private static final String SOURCE_URL = "https://Example.com/jobs/1/";
    private static final JobPostingUrl NORMALIZED_URL = new JobPostingUrl(SOURCE_URL);

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Mock
    private JobPostingAdminQueryRepository queryRepository;

    @Mock
    private JobPostingExtractor extractor;

    private final JobPostingCursorCodec cursorCodec = new JobPostingCursorCodec();

    private JobPostingAdminService service;

    @BeforeEach
    void setUp() {
        service = new JobPostingAdminService(
                jobPostingRepository,
                queryRepository,
                extractor,
                cursorCodec,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void 공고_정보와_역량을_추출한다() {
        when(jobPostingRepository.findIdBySourceUrl(NORMALIZED_URL)).thenReturn(Optional.empty());
        when(extractor.extract("원문")).thenReturn(extracted());

        var response = service.extract(UserRole.ADMIN, new AdminJobPostingExtractRequest(SOURCE_URL, "원문"));

        assertThat(response.companyName()).isEqualTo("회사");
        assertThat(response.requirements()).singleElement()
                .satisfies(requirement -> assertThat(requirement.name()).isEqualTo("Spring"));
        assertThat(response.registeredJobPostingId()).isNull();
    }

    @Test
    void 같은_주소로_등록된_공고가_있으면_추출하면서_그_ID를_알려준다() {
        when(jobPostingRepository.findIdBySourceUrl(NORMALIZED_URL)).thenReturn(Optional.of(7L));
        when(extractor.extract("원문")).thenReturn(extracted());

        var response = service.extract(UserRole.ADMIN, new AdminJobPostingExtractRequest(SOURCE_URL, "원문"));

        assertThat(response.registeredJobPostingId()).isEqualTo(7L);
    }

    @Test
    void 관리자가_아니면_추출하지_않는다() {
        assertThatThrownBy(() -> service.extract(
                UserRole.USER, new AdminJobPostingExtractRequest(SOURCE_URL, "원문")))
                .isInstanceOf(ForbiddenException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_ADMIN_FORBIDDEN);
        verifyNoInteractions(extractor, jobPostingRepository);
    }

    @Test
    void 운영자가_보낸_순서대로_역량에_번호를_붙여_등록한다() {
        when(jobPostingRepository.findIdBySourceUrl(NORMALIZED_URL)).thenReturn(Optional.empty());
        when(jobPostingRepository.save(any(JobPosting.class))).thenReturn(saved());

        var response = service.register(3L, UserRole.ADMIN, saveRequest());

        assertThat(response.jobPostingId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(MatchingStatus.PENDING);
        ArgumentCaptor<JobPosting> captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).save(captor.capture());
        JobPosting registered = captor.getValue();
        assertThat(registered.getSourceUrl()).isEqualTo(NORMALIZED_URL);
        assertThat(registered.getRegisteredBy()).isEqualTo(3L);
        assertThat(registered.getCreatedAt()).isEqualTo(NOW);
        assertThat(registered.getRequirements())
                .extracting(JobRequirement::getDisplayOrder, JobRequirement::getName)
                .containsExactly(
                        Tuple.tuple(1, "Spring"),
                        Tuple.tuple(2, "Kafka")
                );
        verifyNoInteractions(extractor);
    }

    @Test
    void 같은_주소의_공고가_있으면_등록하지_않는다() {
        when(jobPostingRepository.findIdBySourceUrl(NORMALIZED_URL)).thenReturn(Optional.of(7L));

        assertThatThrownBy(() -> service.register(3L, UserRole.ADMIN, saveRequest()))
                .isInstanceOf(DuplicateEntityException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_DUPLICATE_URL);
        verify(jobPostingRepository, never()).save(any());
    }

    @Test
    void 관리자가_아니면_등록하지_않는다() {
        assertThatThrownBy(() -> service.register(3L, UserRole.USER, saveRequest()))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(jobPostingRepository);
    }

    @Test
    void 다음_페이지가_있으면_마지막_공고_ID로_커서를_만든다() {
        when(queryRepository.findAll(null, 2)).thenReturn(new JobPostingPage(
                List.of(summary(9L), summary(8L)), true, 5L));

        var response = service.findAll(UserRole.ADMIN, new AdminJobPostingFindAllRequest(2, null));

        assertThat(response.items()).extracting(item -> item.jobPostingId()).containsExactly(9L, 8L);
        assertThat(response.meta().hasNext()).isTrue();
        assertThat(response.meta().totalCount()).isEqualTo(5L);
        assertThat(cursorCodec.decode(response.meta().nextCursor())).isEqualTo(new JobPostingCursor(8L));
    }

    @Test
    void 커서를_풀어_다음_위치부터_조회하고_마지막_페이지면_커서를_비운다() {
        String cursor = cursorCodec.encode(new JobPostingCursor(8L));
        when(queryRepository.findAll(new JobPostingCursor(8L), 20)).thenReturn(new JobPostingPage(
                List.of(summary(7L)), false, 5L));

        var response = service.findAll(UserRole.ADMIN, new AdminJobPostingFindAllRequest(null, cursor));

        assertThat(response.meta().hasNext()).isFalse();
        assertThat(response.meta().nextCursor()).isNull();
    }

    @Test
    void 관리자가_아니면_목록을_조회하지_않는다() {
        assertThatThrownBy(() -> service.findAll(UserRole.USER, new AdminJobPostingFindAllRequest(null, null)))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(queryRepository);
    }

    @Test
    void 공고_상세를_역량과_함께_조회한다() {
        when(jobPostingRepository.findById(100L)).thenReturn(Optional.of(saved()));

        var response = service.findDetail(100L, UserRole.ADMIN);

        assertThat(response.jobPostingId()).isEqualTo(100L);
        assertThat(response.sourceUrl()).isEqualTo(NORMALIZED_URL.value());
        assertThat(response.requirements()).extracting(requirement -> requirement.requirementId())
                .containsExactly(1000L, 1001L);
    }

    @Test
    void 없는_공고를_조회하면_404를_던진다() {
        when(jobPostingRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findDetail(100L, UserRole.ADMIN))
                .isInstanceOf(EntityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_NOT_FOUND);
    }

    @Test
    void 관리자가_아니면_상세를_조회하지_않는다() {
        assertThatThrownBy(() -> service.findDetail(100L, UserRole.USER))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(jobPostingRepository);
    }

    @Test
    void 원문이_같으면_공고_정보만_고치고_다시_분석하지_않는다() {
        when(jobPostingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(analyzed()));

        var response = service.update(100L, UserRole.ADMIN, updateRequest("원문", "백엔드 개발자 (경력)"));

        assertThat(response).isEqualTo(new AdminJobPostingUpdateResponse(100L, MatchingStatus.READY, false));
        ArgumentCaptor<JobPosting> captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).update(captor.capture(), eq(false));
        JobPosting updated = captor.getValue();
        assertThat(updated.getTitle()).isEqualTo("백엔드 개발자 (경력)");
        assertThat(updated.getUpdatedAt()).isEqualTo(NOW);
        assertThat(updated.getRequirements()).extracting(JobRequirement::getId).containsExactly(1000L);
    }

    @Test
    void 원문이_바뀌면_받은_역량으로_바꾸고_다시_분석한다() {
        when(jobPostingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(analyzed()));

        var response = service.update(100L, UserRole.ADMIN, updateRequest("바뀐 원문", "백엔드 개발자"));

        assertThat(response).isEqualTo(new AdminJobPostingUpdateResponse(100L, MatchingStatus.PENDING, true));
        ArgumentCaptor<JobPosting> captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).update(captor.capture(), eq(true));
        assertThat(captor.getValue().getRequirements())
                .extracting(JobRequirement::getDisplayOrder, JobRequirement::getName, JobRequirement::getStatus)
                .containsExactly(
                        Tuple.tuple(1, "Spring", MatchingStatus.PENDING),
                        Tuple.tuple(2, "Kafka", MatchingStatus.PENDING)
                );
    }

    @Test
    void 공고_주소가_다르면_고치지_않는다() {
        when(jobPostingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(analyzed()));
        AdminJobPostingSaveRequest request = new AdminJobPostingSaveRequest(
                "https://example.com/jobs/2", "원문", "회사", JobRole.BACKEND, "신입", "백엔드 개발자", "정규직",
                LocalDate.of(2026, 12, 31),
                List.of(new AdminJobPostingSaveRequest.Requirement("Spring", RequirementCategory.REQUIRED, "설명"))
        );

        assertThatThrownBy(() -> service.update(100L, UserRole.ADMIN, request))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_URL_MISMATCH);
        verify(jobPostingRepository, never()).update(any(), anyBoolean());
    }

    @Test
    void 없는_공고를_고치면_404를_던진다() {
        when(jobPostingRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(100L, UserRole.ADMIN, saveRequest()))
                .isInstanceOf(EntityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_NOT_FOUND);
        verify(jobPostingRepository, never()).update(any(), anyBoolean());
    }

    @Test
    void 관리자가_아니면_공고를_고치지_않는다() {
        assertThatThrownBy(() -> service.update(100L, UserRole.USER, saveRequest()))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(jobPostingRepository);
    }

    @Test
    void 공고를_숨기고_숨긴_시각을_돌려준다() {
        when(jobPostingRepository.updateHiddenAt(100L, true, NOW))
                .thenReturn(Optional.of(new JobPostingVisibility(100L, NOW)));

        var response = service.updateVisibility(
                100L, UserRole.ADMIN, new AdminJobPostingVisibilityUpdateRequest(true));

        assertThat(response).isEqualTo(new AdminJobPostingVisibilityUpdateResponse(100L, true, NOW));
    }

    @Test
    void 숨김을_해제하면_숨긴_시각을_비워_돌려준다() {
        when(jobPostingRepository.updateHiddenAt(100L, false, NOW))
                .thenReturn(Optional.of(new JobPostingVisibility(100L, null)));

        var response = service.updateVisibility(
                100L, UserRole.ADMIN, new AdminJobPostingVisibilityUpdateRequest(false));

        assertThat(response).isEqualTo(new AdminJobPostingVisibilityUpdateResponse(100L, false, null));
    }

    @Test
    void 없는_공고의_숨김을_바꾸면_404를_던진다() {
        when(jobPostingRepository.updateHiddenAt(100L, true, NOW)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateVisibility(
                100L, UserRole.ADMIN, new AdminJobPostingVisibilityUpdateRequest(true)))
                .isInstanceOf(EntityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_NOT_FOUND);
    }

    @Test
    void 관리자가_아니면_숨김을_바꾸지_않는다() {
        assertThatThrownBy(() -> service.updateVisibility(
                100L, UserRole.USER, new AdminJobPostingVisibilityUpdateRequest(true)))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(jobPostingRepository);
    }

    @Test
    void 공고를_지운다() {
        when(jobPostingRepository.deleteById(100L)).thenReturn(true);

        service.delete(100L, UserRole.ADMIN);

        verify(jobPostingRepository).deleteById(100L);
    }

    @Test
    void 없는_공고를_지우면_404를_던진다() {
        when(jobPostingRepository.deleteById(100L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(100L, UserRole.ADMIN))
                .isInstanceOf(EntityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_NOT_FOUND);
    }

    @Test
    void 관리자가_아니면_공고를_지우지_않는다() {
        assertThatThrownBy(() -> service.delete(100L, UserRole.USER))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(jobPostingRepository);
    }

    private ExtractedJobPosting extracted() {
        return new ExtractedJobPosting(
                "회사", JobRole.BACKEND, "백엔드 개발자", "정규직", "신입", LocalDate.of(2026, 12, 31),
                List.of(new ExtractedJobRequirement("Spring", RequirementCategory.REQUIRED, "설명"))
        );
    }

    private AdminJobPostingSaveRequest saveRequest() {
        return new AdminJobPostingSaveRequest(
                SOURCE_URL, "원문", "회사", JobRole.BACKEND, "신입", "백엔드 개발자", "정규직",
                LocalDate.of(2026, 12, 31),
                List.of(
                        new AdminJobPostingSaveRequest.Requirement("Spring", RequirementCategory.REQUIRED, "설명"),
                        new AdminJobPostingSaveRequest.Requirement("Kafka", RequirementCategory.PREFERRED, "설명")
                )
        );
    }

    private JobPosting saved() {
        return JobPosting.reconstitute(
                100L, NORMALIZED_URL.value(), "a".repeat(64), "회사", JobRole.BACKEND, "신입", "백엔드 개발자",
                "정규직", LocalDate.of(2026, 12, 31), MatchingStatus.PENDING, null, 3L,
                List.of(
                        JobRequirement.reconstitute(1000L, 1, "Spring", RequirementCategory.REQUIRED, "설명",
                                MatchingStatus.PENDING),
                        JobRequirement.reconstitute(1001L, 2, "Kafka", RequirementCategory.PREFERRED, "설명",
                                MatchingStatus.PENDING)
                ),
                NOW, NOW
        );
    }

    private AdminJobPostingSaveRequest updateRequest(String content, String title) {
        return new AdminJobPostingSaveRequest(
                SOURCE_URL, content, "회사", JobRole.BACKEND, "신입", title, "정규직",
                LocalDate.of(2026, 12, 31),
                List.of(
                        new AdminJobPostingSaveRequest.Requirement("Spring", RequirementCategory.REQUIRED, "설명"),
                        new AdminJobPostingSaveRequest.Requirement("Kafka", RequirementCategory.PREFERRED, "설명")
                )
        );
    }

    /**
     * '원문'으로 등록해 분석까지 끝난 공고. 원문이 같은지 보려면 해시가 실제 원문의 해시여야 한다.
     */
    private JobPosting analyzed() {
        return JobPosting.reconstitute(
                100L, NORMALIZED_URL.value(), JobPostingContentHash.from("원문").value(), "회사", JobRole.BACKEND,
                "신입", "백엔드 개발자", "정규직", LocalDate.of(2026, 12, 31), MatchingStatus.READY, null, 3L,
                List.of(JobRequirement.reconstitute(1000L, 1, "Spring", RequirementCategory.REQUIRED, "설명",
                        MatchingStatus.READY)),
                NOW.minusSeconds(3600), NOW.minusSeconds(3600)
        );
    }

    private JobPostingSummary summary(long id) {
        return new JobPostingSummary(id, "회사", "백엔드 개발자", JobRole.BACKEND, MatchingStatus.PENDING, NOW);
    }
}
