package com.shoutoutz.api.verification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.verification.domain.UserVerificationErrorCode;
import com.shoutoutz.api.verification.domain.UserVerificationRequest;
import com.shoutoutz.api.verification.domain.UserVerificationRequestHistory;
import com.shoutoutz.api.verification.domain.UserVerificationRequestHistoryRepository;
import com.shoutoutz.api.verification.domain.UserVerificationRequestRepository;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import com.shoutoutz.api.verification.presentation.dto.request.UserVerificationRequestCreateRequest;
import com.shoutoutz.api.verification.presentation.dto.response.UserVerificationRequestCreateResponse;
import com.shoutoutz.api.verification.presentation.dto.response.UserVerificationRequestResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserVerificationServiceTest {

    private static final long USER_ID = 1L;
    private static final Instant NOW = Instant.parse("2026-09-16T00:00:00Z");
    private static final Instant DECIDED_AT = Instant.parse("2026-09-16T01:00:00Z");

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserVerificationRequestRepository requestRepository;

    @Mock
    private UserVerificationRequestHistoryRepository historyRepository;

    private UserVerificationService userVerificationService;

    @BeforeEach
    void setUp() {
        userVerificationService = new UserVerificationService(
                userProfileRepository,
                requestRepository,
                historyRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void 크루_인증_신청을_저장하고_PENDING_이력을_남긴다() {
        given(userProfileRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(UserProfile.initialize(USER_ID, "일반 사용자")));
        given(requestRepository.findPendingByUserId(USER_ID)).willReturn(Optional.empty());
        UserVerificationRequest savedRequest = UserVerificationRequest.reconstitute(
                10L,
                USER_ID,
                UserType.WOOWACOURSE_CREW,
                "샤를",
                8,
                "BACKEND",
                VerificationRequestStatus.PENDING,
                NOW,
                null
        );
        given(requestRepository.save(any(UserVerificationRequest.class))).willReturn(savedRequest);

        UserVerificationRequestCreateResponse response = userVerificationService.create(
                USER_ID,
                new UserVerificationRequestCreateRequest(
                        UserType.WOOWACOURSE_CREW,
                        "  샤를  ",
                        8,
                        "BACKEND"
                )
        );

        assertThat(response.requestId()).isEqualTo(10L);
        assertThat(response.userType()).isEqualTo(UserType.WOOWACOURSE_CREW);
        assertThat(response.nickname()).isEqualTo("샤를");
        assertThat(response.cohort()).isEqualTo(8);
        assertThat(response.track()).isEqualTo("BACKEND");
        assertThat(response.status()).isEqualTo(VerificationRequestStatus.PENDING);
        assertThat(response.requestedAt()).isEqualTo(NOW);

        ArgumentCaptor<UserVerificationRequestHistory> historyCaptor =
                ArgumentCaptor.forClass(UserVerificationRequestHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        UserVerificationRequestHistory history = historyCaptor.getValue();
        assertThat(history.getRequestId()).isEqualTo(10L);
        assertThat(history.getFromStatus()).isNull();
        assertThat(history.getToStatus()).isEqualTo(VerificationRequestStatus.PENDING);
        assertThat(history.getChangedBy()).isNull();
        assertThat(history.getChangedAt()).isEqualTo(NOW);
    }

    @Test
    void 코치_인증_신청은_기수와_트랙을_null로_저장한다() {
        given(userProfileRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(UserProfile.initialize(USER_ID, "일반 사용자")));
        given(requestRepository.findPendingByUserId(USER_ID)).willReturn(Optional.empty());
        UserVerificationRequest savedRequest = UserVerificationRequest.reconstitute(
                11L,
                USER_ID,
                UserType.WOOWACOURSE_COACH,
                "제임스",
                null,
                null,
                VerificationRequestStatus.PENDING,
                NOW,
                null
        );
        given(requestRepository.save(any(UserVerificationRequest.class))).willReturn(savedRequest);

        UserVerificationRequestCreateResponse response = userVerificationService.create(
                USER_ID,
                new UserVerificationRequestCreateRequest(
                        UserType.WOOWACOURSE_COACH,
                        "제임스",
                        null,
                        null
                )
        );

        assertThat(response.cohort()).isNull();
        assertThat(response.track()).isNull();
        verify(historyRepository).save(any(UserVerificationRequestHistory.class));
    }

    @Test
    void 이미_PENDING_신청이_있으면_새_신청을_만들지_않는다() {
        given(userProfileRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(UserProfile.initialize(USER_ID, "일반 사용자")));
        given(requestRepository.findPendingByUserId(USER_ID))
                .willReturn(Optional.of(UserVerificationRequest.reconstitute(
                        10L,
                        USER_ID,
                        UserType.WOOWACOURSE_CREW,
                        "샤를",
                        8,
                        "BACKEND",
                        VerificationRequestStatus.PENDING,
                        NOW,
                        null
                )));

        assertThatThrownBy(() -> userVerificationService.create(
                USER_ID,
                crewRequest()
        )).isInstanceOfSatisfying(
                ConflictException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(UserVerificationErrorCode.VERIFICATION_REQUEST_ALREADY_PENDING)
        );

        verify(requestRepository, never()).save(any());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void 이미_인증된_사용자는_신청할_수_없다() {
        given(userProfileRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(UserProfile.builder()
                        .userId(USER_ID)
                        .displayName("샤를")
                        .userType(UserType.WOOWACOURSE_CREW)
                        .track("BACKEND")
                        .cohort((short) 8)
                        .build()));

        assertThatThrownBy(() -> userVerificationService.create(
                USER_ID,
                crewRequest()
        )).isInstanceOfSatisfying(
                ConflictException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(UserVerificationErrorCode.VERIFICATION_ALREADY_APPROVED)
        );

        verifyNoInteractions(requestRepository, historyRepository);
    }

    @Test
    void 반려된_신청이_있어도_PENDING이_없으면_새_신청을_저장한다() {
        given(userProfileRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(UserProfile.initialize(USER_ID, "일반 사용자")));
        given(requestRepository.findPendingByUserId(USER_ID)).willReturn(Optional.empty());
        UserVerificationRequest savedRequest = UserVerificationRequest.reconstitute(
                12L,
                USER_ID,
                UserType.WOOWACOURSE_CREW,
                "샤를",
                8,
                "BACKEND",
                VerificationRequestStatus.PENDING,
                NOW,
                null
        );
        given(requestRepository.save(any(UserVerificationRequest.class))).willReturn(savedRequest);

        userVerificationService.create(USER_ID, crewRequest());

        verify(requestRepository).save(any(UserVerificationRequest.class));
        verify(historyRepository).save(any(UserVerificationRequestHistory.class));
    }

    @Test
    void 크루가_기수나_트랙을_누락하면_신청할_수_없다() {
        given(userProfileRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(UserProfile.initialize(USER_ID, "일반 사용자")));
        given(requestRepository.findPendingByUserId(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userVerificationService.create(
                USER_ID,
                new UserVerificationRequestCreateRequest(
                        UserType.WOOWACOURSE_CREW,
                        "샤를",
                        null,
                        "BACKEND"
                )
        )).isInstanceOfSatisfying(
                BadRequestException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(UserVerificationErrorCode.VERIFICATION_COURSE_INFO_INVALID)
        );

        verify(requestRepository, never()).save(any());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void 코치가_기수나_트랙을_보내면_신청할_수_없다() {
        given(userProfileRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(UserProfile.initialize(USER_ID, "일반 사용자")));
        given(requestRepository.findPendingByUserId(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userVerificationService.create(
                USER_ID,
                new UserVerificationRequestCreateRequest(
                        UserType.WOOWACOURSE_COACH,
                        "제임스",
                        8,
                        null
                )
        )).isInstanceOfSatisfying(
                BadRequestException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(UserVerificationErrorCode.VERIFICATION_COURSE_INFO_INVALID)
        );

        verify(requestRepository, never()).save(any());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void 정의되지_않은_트랙을_거절한다() {
        given(userProfileRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(UserProfile.initialize(USER_ID, "일반 사용자")));
        given(requestRepository.findPendingByUserId(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userVerificationService.create(
                USER_ID,
                new UserVerificationRequestCreateRequest(
                        UserType.WOOWACOURSE_CREW,
                        "샤를",
                        8,
                        "BE"
                )
        )).isInstanceOfSatisfying(
                BadRequestException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(UserVerificationErrorCode.VERIFICATION_TRACK_INVALID)
        );

        verify(requestRepository, never()).save(any());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void 신청_이력이_없으면_null을_반환한다() {
        given(userProfileRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(UserProfile.initialize(USER_ID, "일반 사용자")));
        given(requestRepository.findLatestByUserId(USER_ID)).willReturn(Optional.empty());

        assertThat(userVerificationService.findLatest(USER_ID)).isNull();

        verify(requestRepository).findLatestByUserId(USER_ID);
        verifyNoInteractions(historyRepository);
    }

    @Test
    void PENDING_신청은_심사_시각과_반려_사유를_null로_반환한다() {
        UserVerificationRequest request = UserVerificationRequest.reconstitute(
                20L,
                USER_ID,
                UserType.WOOWACOURSE_CREW,
                "샤를",
                8,
                "BACKEND",
                VerificationRequestStatus.PENDING,
                NOW,
                null
        );
        given(userProfileRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(UserProfile.initialize(USER_ID, "일반 사용자")));
        given(requestRepository.findLatestByUserId(USER_ID)).willReturn(Optional.of(request));

        UserVerificationRequestResponse response = userVerificationService.findLatest(USER_ID);

        assertThat(response.requestId()).isEqualTo(20L);
        assertThat(response.status()).isEqualTo(VerificationRequestStatus.PENDING);
        assertThat(response.requestedAt()).isEqualTo(NOW);
        assertThat(response.decidedAt()).isNull();
        assertThat(response.reason()).isNull();
        verifyNoInteractions(historyRepository);
    }

    @Test
    void REJECTED_신청은_최신_반려_이력의_사유와_심사_시각을_반환한다() {
        UserVerificationRequest request = UserVerificationRequest.reconstitute(
                21L,
                USER_ID,
                UserType.WOOWACOURSE_CREW,
                "샤를",
                8,
                "BACKEND",
                VerificationRequestStatus.REJECTED,
                NOW,
                DECIDED_AT
        );
        UserVerificationRequestHistory history = UserVerificationRequestHistory.reconstitute(
                31L,
                21L,
                99L,
                VerificationRequestStatus.PENDING,
                VerificationRequestStatus.REJECTED,
                "Slack 프로필 정보와 일치하지 않습니다.",
                DECIDED_AT
        );
        given(userProfileRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(UserProfile.initialize(USER_ID, "일반 사용자")));
        given(requestRepository.findLatestByUserId(USER_ID)).willReturn(Optional.of(request));
        given(historyRepository.findLatestDecisionByRequestId(21L))
                .willReturn(Optional.of(history));

        UserVerificationRequestResponse response = userVerificationService.findLatest(USER_ID);

        assertThat(response.status()).isEqualTo(VerificationRequestStatus.REJECTED);
        assertThat(response.decidedAt()).isEqualTo(DECIDED_AT);
        assertThat(response.reason()).isEqualTo("Slack 프로필 정보와 일치하지 않습니다.");
    }

    @Test
    void APPROVED_신청은_승인된_신청_정보를_반환한다() {
        UserVerificationRequest request = UserVerificationRequest.reconstitute(
                22L,
                USER_ID,
                UserType.WOOWACOURSE_COACH,
                "제임스",
                null,
                null,
                VerificationRequestStatus.APPROVED,
                NOW,
                null
        );
        UserVerificationRequestHistory history = UserVerificationRequestHistory.reconstitute(
                32L,
                22L,
                99L,
                VerificationRequestStatus.PENDING,
                VerificationRequestStatus.APPROVED,
                null,
                DECIDED_AT
        );
        given(userProfileRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(UserProfile.initialize(USER_ID, "일반 사용자")));
        given(requestRepository.findLatestByUserId(USER_ID)).willReturn(Optional.of(request));
        given(historyRepository.findLatestDecisionByRequestId(22L))
                .willReturn(Optional.of(history));

        UserVerificationRequestResponse response = userVerificationService.findLatest(USER_ID);

        assertThat(response.userType()).isEqualTo(UserType.WOOWACOURSE_COACH);
        assertThat(response.nickname()).isEqualTo("제임스");
        assertThat(response.cohort()).isNull();
        assertThat(response.track()).isNull();
        assertThat(response.status()).isEqualTo(VerificationRequestStatus.APPROVED);
        assertThat(response.decidedAt()).isEqualTo(DECIDED_AT);
        assertThat(response.reason()).isNull();
    }

    @Test
    void 기존_인증_사용자는_신청_이력없이_승인_상태로_반환한다() {
        given(userProfileRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(UserProfile.builder()
                        .userId(USER_ID)
                        .displayName("샤를")
                        .userType(UserType.WOOWACOURSE_CREW)
                        .track("BACKEND")
                        .cohort((short) 8)
                        .build()));

        UserVerificationRequestResponse response = userVerificationService.findLatest(USER_ID);

        assertThat(response.requestId()).isNull();
        assertThat(response.userType()).isEqualTo(UserType.WOOWACOURSE_CREW);
        assertThat(response.nickname()).isEqualTo("샤를");
        assertThat(response.cohort()).isEqualTo(8);
        assertThat(response.track()).isEqualTo("BACKEND");
        assertThat(response.status()).isEqualTo(VerificationRequestStatus.APPROVED);
        assertThat(response.requestedAt()).isNull();
        assertThat(response.decidedAt()).isNull();
        assertThat(response.reason()).isNull();
        verifyNoInteractions(requestRepository, historyRepository);
    }

    private UserVerificationRequestCreateRequest crewRequest() {
        return new UserVerificationRequestCreateRequest(
                UserType.WOOWACOURSE_CREW,
                "샤를",
                8,
                "BACKEND"
        );
    }
}
