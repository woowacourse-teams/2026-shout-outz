package com.shoutoutz.api.jobposting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.jobposting.application.dto.CandidateChunk;
import com.shoutoutz.api.jobposting.application.dto.CandidateChunk.ChunkKind;
import com.shoutoutz.api.jobposting.application.dto.ExtractedJobPosting;
import com.shoutoutz.api.jobposting.application.dto.ExtractedJobRequirement;
import com.shoutoutz.api.jobposting.application.dto.JobPostingAdminCursor;
import com.shoutoutz.api.jobposting.application.dto.JobPostingAdminPage;
import com.shoutoutz.api.jobposting.application.dto.JobPostingAdminSummary;
import com.shoutoutz.api.jobposting.application.dto.RequirementRecommendationDetail;
import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.JobPostingContentHash;
import com.shoutoutz.api.jobposting.domain.JobPostingErrorCode;
import com.shoutoutz.api.jobposting.domain.JobPostingRepository;
import com.shoutoutz.api.jobposting.domain.JobPostingUrl;
import com.shoutoutz.api.jobposting.domain.JobPostingVisibility;
import com.shoutoutz.api.jobposting.domain.JobRequirement;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingFailure;
import com.shoutoutz.api.jobposting.domain.MatchingStatus;
import com.shoutoutz.api.jobposting.domain.ReanalysisScope;
import com.shoutoutz.api.jobposting.domain.RecommendationVerdict;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingExtractRequest;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingFindAllRequest;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingReanalysisRequest;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingSaveRequest;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingUpdateRequest;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingVisibilityUpdateRequest;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingReanalysisResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingUpdateResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingVisibilityUpdateResponse;
import com.shoutoutz.api.media.application.MediaUrlResolver;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

    @Mock
    private MediaMetadataRepository mediaMetadataRepository;

    @Mock
    private MediaUrlResolver mediaUrlResolver;

    private final JobPostingAdminCursorCodec cursorCodec = new JobPostingAdminCursorCodec();

    private JobPostingAdminService service;

    @BeforeEach
    void setUp() {
        service = new JobPostingAdminService(
                jobPostingRepository,
                queryRepository,
                extractor,
                cursorCodec,
                mediaMetadataRepository,
                mediaUrlResolver,
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
        when(queryRepository.findAll(null, 2)).thenReturn(new JobPostingAdminPage(
                List.of(summary(9L), summary(8L)), true, 5L));

        var response = service.findAll(UserRole.ADMIN, new AdminJobPostingFindAllRequest(2, null));

        assertThat(response.items()).extracting(item -> item.jobPostingId()).containsExactly(9L, 8L);
        assertThat(response.meta().hasNext()).isTrue();
        assertThat(response.meta().totalCount()).isEqualTo(5L);
        assertThat(cursorCodec.decode(response.meta().nextCursor())).isEqualTo(new JobPostingAdminCursor(8L));
    }

    @Test
    void 커서를_풀어_다음_위치부터_조회하고_마지막_페이지면_커서를_비운다() {
        String cursor = cursorCodec.encode(new JobPostingAdminCursor(8L));
        when(queryRepository.findAll(new JobPostingAdminCursor(8L), 20)).thenReturn(new JobPostingAdminPage(
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
    void 공고_상세의_역량마다_실패_이유와_추천_자료를_읽은_순서대로_담는다() {
        when(jobPostingRepository.findById(100L)).thenReturn(Optional.of(saved()));
        when(queryRepository.findLastErrors(100L)).thenReturn(Map.of(1001L, MatchingFailure.JUDGE_FAILED));
        when(queryRepository.findRecommendations(100L)).thenReturn(List.of(
                recommendationDetail(1000L, "match", RecommendationVerdict.MATCH),
                recommendationDetail(1000L, "not-match", RecommendationVerdict.NOT_MATCH)
        ));

        var response = service.findDetail(100L, UserRole.ADMIN);

        var first = response.requirements().get(0);
        assertThat(first.lastError()).isNull();
        assertThat(first.recommendations()).extracting(recommendation -> recommendation.externalKey())
                .containsExactly("match", "not-match");
        assertThat(first.recommendations().getFirst().evidence()).singleElement()
                .satisfies(evidence -> {
                    assertThat(evidence.kind()).isEqualTo(ChunkKind.EXPERIENCE);
                    assertThat(evidence.text()).isEqualTo("경험 조각");
                });
        var second = response.requirements().get(1);
        assertThat(second.lastError()).isEqualTo(MatchingFailure.JUDGE_FAILED);
        assertThat(second.recommendations()).isEmpty();
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
    void 원문_없이_고치면_공고_정보만_고치고_다시_분석하지_않는다() {
        when(jobPostingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(analyzed()));

        var response = service.update(100L, UserRole.ADMIN, updateRequest(null, "백엔드 개발자 (경력)"));

        assertThat(response).isEqualTo(new AdminJobPostingUpdateResponse(100L, MatchingStatus.READY, false));
        ArgumentCaptor<JobPosting> captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).update(captor.capture(), eq(false));
        JobPosting updated = captor.getValue();
        assertThat(updated.getTitle()).isEqualTo("백엔드 개발자 (경력)");
        assertThat(updated.getRequirements()).extracting(JobRequirement::getId).containsExactly(1000L);
    }

    @Test
    void 원문과_역량_없이_고치면_기존_역량을_그대로_둔다() {
        when(jobPostingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(analyzed()));
        AdminJobPostingUpdateRequest request = new AdminJobPostingUpdateRequest(
                SOURCE_URL, null, "회사", JobRole.BACKEND, "신입", "백엔드 개발자", "정규직",
                LocalDate.of(2026, 12, 31), null, null
        );

        var response = service.update(100L, UserRole.ADMIN, request);

        assertThat(response).isEqualTo(new AdminJobPostingUpdateResponse(100L, MatchingStatus.READY, false));
        ArgumentCaptor<JobPosting> captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).update(captor.capture(), eq(false));
        assertThat(captor.getValue().getRequirements()).extracting(JobRequirement::getId).containsExactly(1000L);
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
        AdminJobPostingUpdateRequest request = new AdminJobPostingUpdateRequest(
                "https://example.com/jobs/2", "원문", "회사", JobRole.BACKEND, "신입", "백엔드 개발자", "정규직",
                LocalDate.of(2026, 12, 31), null,
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

        assertThatThrownBy(() -> service.update(100L, UserRole.ADMIN, updateRequest("원문", "백엔드 개발자")))
                .isInstanceOf(EntityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_NOT_FOUND);
        verify(jobPostingRepository, never()).update(any(), anyBoolean());
    }

    @Test
    void 관리자가_아니면_공고를_고치지_않는다() {
        assertThatThrownBy(() -> service.update(100L, UserRole.USER, updateRequest("원문", "백엔드 개발자")))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(jobPostingRepository);
    }

    @Test
    void 회사_로고를_골라_등록하면_로고와_함께_저장한다() {
        when(mediaMetadataRepository.findById(50L))
                .thenReturn(Optional.of(media(50L, MediaPurpose.JOB_POSTING_COMPANY_LOGO, MediaStatus.READY)));
        when(jobPostingRepository.findIdBySourceUrl(NORMALIZED_URL)).thenReturn(Optional.empty());
        when(jobPostingRepository.save(any(JobPosting.class))).thenReturn(saved());

        service.register(3L, UserRole.ADMIN, saveRequest(50L));

        ArgumentCaptor<JobPosting> captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).save(captor.capture());
        assertThat(captor.getValue().getLogoMediaId()).isEqualTo(50L);
    }

    @Test
    void 회사_로고_없이_등록하면_이미지를_찾지_않는다() {
        when(jobPostingRepository.findIdBySourceUrl(NORMALIZED_URL)).thenReturn(Optional.empty());
        when(jobPostingRepository.save(any(JobPosting.class))).thenReturn(saved());

        service.register(3L, UserRole.ADMIN, saveRequest());

        ArgumentCaptor<JobPosting> captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).save(captor.capture());
        assertThat(captor.getValue().getLogoMediaId()).isNull();
        verifyNoInteractions(mediaMetadataRepository);
    }

    @Test
    void 없는_이미지를_회사_로고로_등록하면_400을_던진다() {
        when(mediaMetadataRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(3L, UserRole.ADMIN, saveRequest(50L)))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_INVALID_LOGO);
        verify(jobPostingRepository, never()).save(any());
    }

    @Test
    void 다른_용도의_이미지를_회사_로고로_등록하면_400을_던진다() {
        when(mediaMetadataRepository.findById(50L))
                .thenReturn(Optional.of(media(50L, MediaPurpose.PROJECT_LOGO, MediaStatus.READY)));

        assertThatThrownBy(() -> service.register(3L, UserRole.ADMIN, saveRequest(50L)))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_INVALID_LOGO);
        verify(jobPostingRepository, never()).save(any());
    }

    @Test
    void 처리가_끝나지_않은_이미지를_회사_로고로_등록하면_400을_던진다() {
        when(mediaMetadataRepository.findById(50L))
                .thenReturn(Optional.of(
                        media(50L, MediaPurpose.JOB_POSTING_COMPANY_LOGO, MediaStatus.PROCESSING)));

        assertThatThrownBy(() -> service.register(3L, UserRole.ADMIN, saveRequest(50L)))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_LOGO_NOT_READY);
        verify(jobPostingRepository, never()).save(any());
    }

    @Test
    void 공고를_고칠_때_받은_회사_로고로_바꾼다() {
        when(mediaMetadataRepository.findById(51L))
                .thenReturn(Optional.of(media(51L, MediaPurpose.JOB_POSTING_COMPANY_LOGO, MediaStatus.READY)));
        when(jobPostingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(analyzed(50L)));

        service.update(100L, UserRole.ADMIN, updateRequest("원문", "백엔드 개발자", 51L));

        ArgumentCaptor<JobPosting> captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).update(captor.capture(), eq(false));
        assertThat(captor.getValue().getLogoMediaId()).isEqualTo(51L);
    }

    @Test
    void 공고를_고칠_때_회사_로고를_비워_보내면_로고를_지운다() {
        when(jobPostingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(analyzed(50L)));

        service.update(100L, UserRole.ADMIN, updateRequest("원문", "백엔드 개발자"));

        ArgumentCaptor<JobPosting> captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).update(captor.capture(), eq(false));
        assertThat(captor.getValue().getLogoMediaId()).isNull();
        verifyNoInteractions(mediaMetadataRepository);
    }

    @Test
    void 공고를_고칠_때_없는_이미지를_회사_로고로_보내면_400을_던진다() {
        when(mediaMetadataRepository.findById(51L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                100L, UserRole.ADMIN, updateRequest("원문", "백엔드 개발자", 51L)))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_INVALID_LOGO);
        verify(jobPostingRepository, never()).update(any(), anyBoolean());
    }

    @Test
    void 공고를_고칠_때_다른_용도의_이미지를_회사_로고로_보내면_400을_던진다() {
        when(mediaMetadataRepository.findById(51L))
                .thenReturn(Optional.of(media(51L, MediaPurpose.HOME_BANNER, MediaStatus.READY)));

        assertThatThrownBy(() -> service.update(
                100L, UserRole.ADMIN, updateRequest("원문", "백엔드 개발자", 51L)))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_INVALID_LOGO);
        verify(jobPostingRepository, never()).update(any(), anyBoolean());
    }

    @Test
    void 공고를_고칠_때_처리가_끝나지_않은_이미지를_회사_로고로_보내면_400을_던진다() {
        when(mediaMetadataRepository.findById(51L))
                .thenReturn(Optional.of(media(51L, MediaPurpose.JOB_POSTING_COMPANY_LOGO, MediaStatus.FAILED)));

        assertThatThrownBy(() -> service.update(
                100L, UserRole.ADMIN, updateRequest("원문", "백엔드 개발자", 51L)))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_LOGO_NOT_READY);
        verify(jobPostingRepository, never()).update(any(), anyBoolean());
    }

    @Test
    void 공고_상세에_회사_로고_ID와_작은_이미지_주소를_담는다() {
        when(jobPostingRepository.findById(100L)).thenReturn(Optional.of(analyzed(50L)));
        when(mediaUrlResolver.resolve(50L, MediaVariant.THUMBNAIL))
                .thenReturn(URI.create("https://cdn.example.com/media/logo/50-thumbnail.webp"));

        var response = service.findDetail(100L, UserRole.ADMIN);

        assertThat(response.logoImageId()).isEqualTo(50L);
        assertThat(response.logoUrl()).isEqualTo("https://cdn.example.com/media/logo/50-thumbnail.webp");
    }

    @Test
    void 회사_로고가_없는_공고_상세는_로고_주소를_비운다() {
        when(jobPostingRepository.findById(100L)).thenReturn(Optional.of(saved()));

        var response = service.findDetail(100L, UserRole.ADMIN);

        assertThat(response.logoImageId()).isNull();
        assertThat(response.logoUrl()).isNull();
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

    @Test
    void 실패한_공고는_다른_역량이_분석_중이어도_실패한_역량만_다시_분석한다() {
        when(jobPostingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(postingWith(
                MatchingStatus.FAILED, MatchingStatus.READY, MatchingStatus.FAILED, MatchingStatus.PROCESSING)));
        when(jobPostingRepository.resetForReanalysis(100L, ReanalysisScope.FAILED, NOW)).thenReturn(1);

        var response = service.reanalyze(
                100L, UserRole.ADMIN, new AdminJobPostingReanalysisRequest(ReanalysisScope.FAILED));

        assertThat(response).isEqualTo(new AdminJobPostingReanalysisResponse(100L, MatchingStatus.PENDING, 1));
    }

    @ParameterizedTest
    @EnumSource(value = MatchingStatus.class, names = {"READY", "FAILED"})
    void 분석_중인_역량이_없으면_모든_역량을_다시_분석한다(MatchingStatus postingStatus) {
        when(jobPostingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(postingWith(
                postingStatus, MatchingStatus.READY, MatchingStatus.FAILED)));
        when(jobPostingRepository.resetForReanalysis(100L, ReanalysisScope.ALL, NOW)).thenReturn(2);

        var response = service.reanalyze(
                100L, UserRole.ADMIN, new AdminJobPostingReanalysisRequest(ReanalysisScope.ALL));

        assertThat(response).isEqualTo(new AdminJobPostingReanalysisResponse(100L, MatchingStatus.PENDING, 2));
    }

    @ParameterizedTest
    @EnumSource(value = MatchingStatus.class, names = {"PENDING", "PROCESSING", "READY"})
    void 실패하지_않은_공고는_실패한_역량_다시_분석을_거절한다(MatchingStatus postingStatus) {
        when(jobPostingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(postingWith(
                postingStatus, MatchingStatus.READY, MatchingStatus.FAILED)));

        assertThatThrownBy(() -> service.reanalyze(
                100L, UserRole.ADMIN, new AdminJobPostingReanalysisRequest(ReanalysisScope.FAILED)))
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_NOT_FAILED);
        verify(jobPostingRepository, never()).resetForReanalysis(anyLong(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = MatchingStatus.class, names = {"PENDING", "PROCESSING"})
    void 분석_중인_역량이_있으면_모든_역량_다시_분석을_거절한다(MatchingStatus requirementStatus) {
        when(jobPostingRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(postingWith(
                MatchingStatus.FAILED, MatchingStatus.FAILED, requirementStatus)));

        assertThatThrownBy(() -> service.reanalyze(
                100L, UserRole.ADMIN, new AdminJobPostingReanalysisRequest(ReanalysisScope.ALL)))
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_ANALYSIS_IN_PROGRESS);
        verify(jobPostingRepository, never()).resetForReanalysis(anyLong(), any(), any());
    }

    @Test
    void 없는_공고를_다시_분석하면_404를_던진다() {
        when(jobPostingRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reanalyze(
                100L, UserRole.ADMIN, new AdminJobPostingReanalysisRequest(ReanalysisScope.ALL)))
                .isInstanceOf(EntityNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_NOT_FOUND);
        verify(jobPostingRepository, never()).resetForReanalysis(anyLong(), any(), any());
    }

    @Test
    void 관리자가_아니면_다시_분석하지_않는다() {
        assertThatThrownBy(() -> service.reanalyze(
                100L, UserRole.USER, new AdminJobPostingReanalysisRequest(ReanalysisScope.ALL)))
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
        return saveRequest(null);
    }

    private AdminJobPostingSaveRequest saveRequest(Long logoImageId) {
        return new AdminJobPostingSaveRequest(
                SOURCE_URL, "원문", "회사", JobRole.BACKEND, "신입", "백엔드 개발자", "정규직",
                LocalDate.of(2026, 12, 31), logoImageId,
                List.of(
                        new AdminJobPostingSaveRequest.Requirement("Spring", RequirementCategory.REQUIRED, "설명"),
                        new AdminJobPostingSaveRequest.Requirement("Kafka", RequirementCategory.PREFERRED, "설명")
                )
        );
    }

    private JobPosting saved() {
        return JobPosting.reconstitute(
                100L, NORMALIZED_URL.value(), "a".repeat(64), "회사", JobRole.BACKEND, "신입", "백엔드 개발자",
                "정규직", LocalDate.of(2026, 12, 31), null, MatchingStatus.PENDING, null, 3L,
                List.of(
                        JobRequirement.reconstitute(1000L, 1, "Spring", RequirementCategory.REQUIRED, "설명",
                                MatchingStatus.PENDING),
                        JobRequirement.reconstitute(1001L, 2, "Kafka", RequirementCategory.PREFERRED, "설명",
                                MatchingStatus.PENDING)
                ),
                NOW, NOW
        );
    }

    /**
     * 역량 id 는 1000 부터 차례로 붙는다.
     */
    private JobPosting postingWith(MatchingStatus postingStatus, MatchingStatus... requirementStatuses) {
        return JobPosting.reconstitute(
                100L, NORMALIZED_URL.value(), "a".repeat(64), "회사", JobRole.BACKEND, "신입", "백엔드 개발자",
                "정규직", LocalDate.of(2026, 12, 31), null, postingStatus, null, 3L,
                IntStream.range(0, requirementStatuses.length)
                        .mapToObj(index -> JobRequirement.reconstitute(1000L + index, index + 1, "역량" + index,
                                RequirementCategory.REQUIRED, "설명", requirementStatuses[index]))
                        .toList(),
                NOW, NOW
        );
    }

    private RequirementRecommendationDetail recommendationDetail(long requirementId, String externalKey,
                                                                 RecommendationVerdict verdict) {
        return new RequirementRecommendationDetail(
                requirementId, "github-pr", externalKey, "자료 제목", "https://example.com/" + externalKey,
                verdict, "판정 이유", "설명", 0.7,
                List.of(new CandidateChunk(ChunkKind.EXPERIENCE, "경험 조각", 0.7))
        );
    }

    private AdminJobPostingUpdateRequest updateRequest(String content, String title) {
        return updateRequest(content, title, null);
    }

    private AdminJobPostingUpdateRequest updateRequest(String content, String title, Long logoImageId) {
        return new AdminJobPostingUpdateRequest(
                SOURCE_URL, content, "회사", JobRole.BACKEND, "신입", title, "정규직",
                LocalDate.of(2026, 12, 31), logoImageId,
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
        return analyzed(null);
    }

    private JobPosting analyzed(Long logoMediaId) {
        return JobPosting.reconstitute(
                100L, NORMALIZED_URL.value(), JobPostingContentHash.from("원문").value(), "회사", JobRole.BACKEND,
                "신입", "백엔드 개발자", "정규직", LocalDate.of(2026, 12, 31), logoMediaId, MatchingStatus.READY, null,
                3L,
                List.of(JobRequirement.reconstitute(1000L, 1, "Spring", RequirementCategory.REQUIRED, "설명",
                        MatchingStatus.READY)),
                NOW.minusSeconds(3600), NOW.minusSeconds(3600)
        );
    }

    private MediaMetadata media(long id, MediaPurpose purpose, MediaStatus status) {
        return MediaMetadata.reconstitute(
                id, 3L, purpose, "media/logo/" + id, null, "image/webp", 1024L, status,
                NOW, null, NOW, NOW, NOW
        );
    }

    private JobPostingAdminSummary summary(long id) {
        return new JobPostingAdminSummary(id, "회사", "백엔드 개발자", JobRole.BACKEND, MatchingStatus.PENDING, 0, null, NOW);
    }
}
