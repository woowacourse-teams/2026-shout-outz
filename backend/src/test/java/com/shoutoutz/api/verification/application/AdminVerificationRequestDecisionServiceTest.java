package com.shoutoutz.api.verification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.verification.domain.UserVerificationErrorCode;
import com.shoutoutz.api.verification.domain.UserVerificationRequest;
import com.shoutoutz.api.verification.domain.UserVerificationRequestHistory;
import com.shoutoutz.api.verification.domain.UserVerificationRequestHistoryRepository;
import com.shoutoutz.api.verification.domain.UserVerificationRequestRepository;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestApproveResponse;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestRejectResponse;
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
class AdminVerificationRequestDecisionServiceTest {

    private static final long REQUEST_ID = 101L;
    private static final long USER_ID = 42L;
    private static final long ADMIN_ID = 7L;
    private static final Instant NOW = Instant.parse("2026-09-16T03:10:00Z");

    @Mock
    private UserVerificationRequestRepository requestRepository;

    @Mock
    private UserVerificationRequestHistoryRepository historyRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserRepository userRepository;

    private AdminVerificationRequestDecisionService decisionService;

    @BeforeEach
    void setUp() {
        decisionService = new AdminVerificationRequestDecisionService(
                requestRepository,
                historyRepository,
                userProfileRepository,
                userRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void 관리자가_크루_인증_신청을_승인하고_프로필과_이력을_변경한다() {
        UserVerificationRequest request = crewRequest();
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(request));
        given(userProfileRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(UserProfile.initialize(USER_ID, "일반 사용자")));
        given(userRepository.findById(ADMIN_ID)).willReturn(Optional.of(admin()));
        given(requestRepository.save(any(UserVerificationRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(userProfileRepository.save(any(UserProfile.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(historyRepository.save(any(UserVerificationRequestHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        AdminVerificationRequestApproveResponse response = decisionService.approve(
                REQUEST_ID,
                ADMIN_ID,
                UserRole.ADMIN
        );

        assertThat(response.requestId()).isEqualTo(REQUEST_ID);
        assertThat(response.status()).isEqualTo(VerificationRequestStatus.APPROVED);
        assertThat(response.decidedBy().userId()).isEqualTo(ADMIN_ID);
        assertThat(response.decidedBy().handle()).isEqualTo("admin");
        assertThat(response.decidedAt()).isEqualTo(NOW);

        ArgumentCaptor<UserVerificationRequest> requestCaptor =
                ArgumentCaptor.forClass(UserVerificationRequest.class);
        verify(requestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStatus())
                .isEqualTo(VerificationRequestStatus.APPROVED);
        assertThat(requestCaptor.getValue().getDecidedAt()).isEqualTo(NOW);

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getDisplayName().value()).isEqualTo("8기 샤를");
        assertThat(profileCaptor.getValue().getUserType()).isEqualTo(UserType.WOOWACOURSE_CREW);
        assertThat(profileCaptor.getValue().getCohort()).isEqualTo((short) 8);
        assertThat(profileCaptor.getValue().getTrack()).isEqualTo("BACKEND");

        ArgumentCaptor<UserVerificationRequestHistory> historyCaptor =
                ArgumentCaptor.forClass(UserVerificationRequestHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getFromStatus())
                .isEqualTo(VerificationRequestStatus.PENDING);
        assertThat(historyCaptor.getValue().getToStatus())
                .isEqualTo(VerificationRequestStatus.APPROVED);
        assertThat(historyCaptor.getValue().getChangedBy()).isEqualTo(ADMIN_ID);
        assertThat(historyCaptor.getValue().getReason()).isNull();
        assertThat(historyCaptor.getValue().getChangedAt()).isEqualTo(NOW);
    }

    @Test
    void 코치_인증_신청을_승인하면_프로필에_기수와_트랙을_저장하지_않는다() {
        UserVerificationRequest request = UserVerificationRequest.reconstitute(
                REQUEST_ID,
                USER_ID,
                UserType.WOOWACOURSE_COACH,
                "제임스",
                null,
                null,
                VerificationRequestStatus.PENDING,
                NOW,
                null
        );
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(request));
        given(userProfileRepository.findByUserId(USER_ID))
                .willReturn(Optional.of(UserProfile.initialize(USER_ID, "일반 사용자")));
        given(userRepository.findById(ADMIN_ID)).willReturn(Optional.of(admin()));
        given(requestRepository.save(any(UserVerificationRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(userProfileRepository.save(any(UserProfile.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(historyRepository.save(any(UserVerificationRequestHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        decisionService.approve(REQUEST_ID, ADMIN_ID, UserRole.ADMIN);

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getDisplayName().value()).isEqualTo("제임스");
        assertThat(profileCaptor.getValue().getUserType()).isEqualTo(UserType.WOOWACOURSE_COACH);
        assertThat(profileCaptor.getValue().getCohort()).isNull();
        assertThat(profileCaptor.getValue().getTrack()).isNull();
    }

    @Test
    void 관리자가_인증_신청을_반려하고_프로필은_변경하지_않으며_이력을_남긴다() {
        String reason = "Slack 프로필의 기수 정보와 일치하지 않습니다.";
        UserVerificationRequest request = crewRequest();
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(request));
        given(userRepository.findById(ADMIN_ID)).willReturn(Optional.of(admin()));
        given(requestRepository.save(any(UserVerificationRequest.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(historyRepository.save(any(UserVerificationRequestHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        AdminVerificationRequestRejectResponse response = decisionService.reject(
                REQUEST_ID,
                ADMIN_ID,
                UserRole.ADMIN,
                reason
        );

        assertThat(response.requestId()).isEqualTo(REQUEST_ID);
        assertThat(response.status()).isEqualTo(VerificationRequestStatus.REJECTED);
        assertThat(response.reason()).isEqualTo(reason);
        assertThat(response.decidedBy().userId()).isEqualTo(ADMIN_ID);
        assertThat(response.decidedBy().handle()).isEqualTo("admin");
        assertThat(response.decidedAt()).isEqualTo(NOW);

        ArgumentCaptor<UserVerificationRequest> requestCaptor =
                ArgumentCaptor.forClass(UserVerificationRequest.class);
        verify(requestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStatus())
                .isEqualTo(VerificationRequestStatus.REJECTED);
        assertThat(requestCaptor.getValue().getDecidedAt()).isEqualTo(NOW);

        ArgumentCaptor<UserVerificationRequestHistory> historyCaptor =
                ArgumentCaptor.forClass(UserVerificationRequestHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getFromStatus())
                .isEqualTo(VerificationRequestStatus.PENDING);
        assertThat(historyCaptor.getValue().getToStatus())
                .isEqualTo(VerificationRequestStatus.REJECTED);
        assertThat(historyCaptor.getValue().getChangedBy()).isEqualTo(ADMIN_ID);
        assertThat(historyCaptor.getValue().getReason()).isEqualTo(reason);
        assertThat(historyCaptor.getValue().getChangedAt()).isEqualTo(NOW);

        verifyNoInteractions(userProfileRepository);
    }

    @Test
    void 관리자가_아니면_승인할_수_없다() {
        assertThatThrownBy(() -> decisionService.approve(
                REQUEST_ID,
                ADMIN_ID,
                UserRole.USER
        )).isInstanceOfSatisfying(
                ForbiddenException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(UserVerificationErrorCode.VERIFICATION_ADMIN_FORBIDDEN)
        );

        verifyNoInteractions(
                requestRepository,
                historyRepository,
                userProfileRepository,
                userRepository
        );
    }

    @Test
    void 존재하지_않는_신청은_승인할_수_없다() {
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> decisionService.approve(
                REQUEST_ID,
                ADMIN_ID,
                UserRole.ADMIN
        )).isInstanceOfSatisfying(
                EntityNotFoundException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(UserVerificationErrorCode.VERIFICATION_REQUEST_NOT_FOUND)
        );

        verifyNoInteractions(historyRepository, userProfileRepository, userRepository);
    }

    @Test
    void PENDING이_아닌_신청은_승인할_수_없다() {
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(
                UserVerificationRequest.reconstitute(
                        REQUEST_ID,
                        USER_ID,
                        UserType.WOOWACOURSE_CREW,
                        "샤를",
                        8,
                        "BACKEND",
                        VerificationRequestStatus.REJECTED,
                        NOW,
                        NOW
                )
        ));

        assertThatThrownBy(() -> decisionService.approve(
                REQUEST_ID,
                ADMIN_ID,
                UserRole.ADMIN
        )).isInstanceOfSatisfying(
                ConflictException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(UserVerificationErrorCode.VERIFICATION_REQUEST_NOT_PENDING)
        );

        verify(userProfileRepository, never()).findByUserId(any(Long.class));
        verifyNoInteractions(historyRepository, userRepository);
    }

    @Test
    void PENDING이_아닌_신청은_반려할_수_없다() {
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(
                UserVerificationRequest.reconstitute(
                        REQUEST_ID,
                        USER_ID,
                        UserType.WOOWACOURSE_CREW,
                        "샤를",
                        8,
                        "BACKEND",
                        VerificationRequestStatus.APPROVED,
                        NOW,
                        NOW
                )
        ));

        assertThatThrownBy(() -> decisionService.reject(
                REQUEST_ID,
                ADMIN_ID,
                UserRole.ADMIN,
                "반려 사유"
        )).isInstanceOfSatisfying(
                ConflictException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(UserVerificationErrorCode.VERIFICATION_REQUEST_NOT_PENDING)
        );

        verifyNoInteractions(historyRepository, userRepository, userProfileRepository);
    }

    private UserVerificationRequest crewRequest() {
        return UserVerificationRequest.reconstitute(
                REQUEST_ID,
                USER_ID,
                UserType.WOOWACOURSE_CREW,
                "샤를",
                8,
                "BACKEND",
                VerificationRequestStatus.PENDING,
                NOW,
                null
        );
    }

    private User admin() {
        return User.builder()
                .id(ADMIN_ID)
                .handle("admin")
                .status(UserStatus.ACTIVE)
                .role(UserRole.ADMIN)
                .build();
    }
}
