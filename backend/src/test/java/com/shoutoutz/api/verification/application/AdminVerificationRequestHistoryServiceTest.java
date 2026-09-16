package com.shoutoutz.api.verification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.verification.domain.UserVerificationErrorCode;
import com.shoutoutz.api.verification.domain.UserVerificationRequest;
import com.shoutoutz.api.verification.domain.UserVerificationRequestHistory;
import com.shoutoutz.api.verification.domain.UserVerificationRequestHistoryRepository;
import com.shoutoutz.api.verification.domain.UserVerificationRequestRepository;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestHistoryResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminVerificationRequestHistoryServiceTest {

    private static final long REQUEST_ID = 101L;
    private static final long USER_ID = 42L;
    private static final long ADMIN_ID = 7L;
    private static final Instant REQUESTED_AT = Instant.parse("2026-09-16T02:30:00Z");
    private static final Instant DECIDED_AT = Instant.parse("2026-09-16T03:00:00Z");

    @Mock
    private UserVerificationRequestRepository requestRepository;

    @Mock
    private UserVerificationRequestHistoryRepository historyRepository;

    @Mock
    private UserRepository userRepository;

    private AdminVerificationRequestHistoryService historyService;

    @BeforeEach
    void setUp() {
        historyService = new AdminVerificationRequestHistoryService(
                requestRepository,
                historyRepository,
                userRepository
        );
    }

    @Test
    void 관리자가_특정_신청의_전체_이력을_최신순으로_조회한다() {
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.of(request()));
        given(historyRepository.findAllByRequestId(REQUEST_ID)).willReturn(List.of(
                UserVerificationRequestHistory.reconstitute(
                        202L,
                        REQUEST_ID,
                        ADMIN_ID,
                        VerificationRequestStatus.PENDING,
                        VerificationRequestStatus.REJECTED,
                        "Slack 프로필의 기수 정보와 일치하지 않습니다.",
                        DECIDED_AT
                ),
                UserVerificationRequestHistory.reconstitute(
                        201L,
                        REQUEST_ID,
                        null,
                        null,
                        VerificationRequestStatus.PENDING,
                        null,
                        REQUESTED_AT
                )
        ));
        given(userRepository.findById(ADMIN_ID)).willReturn(Optional.of(admin()));

        AdminVerificationRequestHistoryResponse response = historyService.findHistory(
                REQUEST_ID,
                UserRole.ADMIN
        );

        assertThat(response.requestId()).isEqualTo(REQUEST_ID);
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0)).satisfies(item -> {
            assertThat(item.historyId()).isEqualTo(202L);
            assertThat(item.fromStatus()).isEqualTo(VerificationRequestStatus.PENDING);
            assertThat(item.toStatus()).isEqualTo(VerificationRequestStatus.REJECTED);
            assertThat(item.changedBy().userId()).isEqualTo(ADMIN_ID);
            assertThat(item.changedBy().handle()).isEqualTo("admin");
            assertThat(item.reason()).isEqualTo("Slack 프로필의 기수 정보와 일치하지 않습니다.");
            assertThat(item.changedAt()).isEqualTo(DECIDED_AT);
        });
        assertThat(response.items().get(1)).satisfies(item -> {
            assertThat(item.historyId()).isEqualTo(201L);
            assertThat(item.fromStatus()).isNull();
            assertThat(item.toStatus()).isEqualTo(VerificationRequestStatus.PENDING);
            assertThat(item.changedBy()).isNull();
            assertThat(item.reason()).isNull();
            assertThat(item.changedAt()).isEqualTo(REQUESTED_AT);
        });
    }

    @Test
    void 일반_사용자는_신청_이력을_조회할_수_없다() {
        assertThatThrownBy(() -> historyService.findHistory(REQUEST_ID, UserRole.USER))
                .isInstanceOfSatisfying(
                        ForbiddenException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(UserVerificationErrorCode.VERIFICATION_ADMIN_FORBIDDEN)
                );

        verifyNoInteractions(requestRepository, historyRepository, userRepository);
    }

    @Test
    void 존재하지_않는_신청의_이력은_조회할_수_없다() {
        given(requestRepository.findById(REQUEST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> historyService.findHistory(REQUEST_ID, UserRole.ADMIN))
                .isInstanceOfSatisfying(
                        EntityNotFoundException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(UserVerificationErrorCode.VERIFICATION_REQUEST_NOT_FOUND)
                );

        verifyNoInteractions(historyRepository, userRepository);
    }

    private UserVerificationRequest request() {
        return UserVerificationRequest.reconstitute(
                REQUEST_ID,
                USER_ID,
                UserType.WOOWACOURSE_CREW,
                "샤를",
                8,
                "BACKEND",
                VerificationRequestStatus.REJECTED,
                REQUESTED_AT,
                DECIDED_AT
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
