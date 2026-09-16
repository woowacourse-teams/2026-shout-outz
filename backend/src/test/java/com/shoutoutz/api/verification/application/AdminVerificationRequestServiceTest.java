package com.shoutoutz.api.verification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.verification.application.dto.AdminVerificationRequestCursor;
import com.shoutoutz.api.verification.application.dto.AdminVerificationRequestItem;
import com.shoutoutz.api.verification.domain.UserVerificationErrorCode;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import com.shoutoutz.api.verification.presentation.dto.request.AdminVerificationRequestFindAllRequest;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestFindAllResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminVerificationRequestServiceTest {

    private static final Instant LATEST_REQUESTED_AT = Instant.parse("2026-09-16T02:30:00Z");

    @Mock
    private AdminVerificationRequestQueryRepository queryRepository;

    private AdminVerificationRequestCursorCodec cursorCodec;
    private AdminVerificationRequestService service;

    @BeforeEach
    void setUp() {
        cursorCodec = new AdminVerificationRequestCursorCodec();
        service = new AdminVerificationRequestService(queryRepository, cursorCodec);
    }

    @Test
    void 관리자_인증_신청_목록을_상태_기본값과_다음_커서로_조회한다() {
        AdminVerificationRequestFindAllRequest request =
                new AdminVerificationRequestFindAllRequest(null, 2, null);
        AdminVerificationRequestItem first = item(
                101L,
                LATEST_REQUESTED_AT,
                VerificationRequestStatus.PENDING
        );
        AdminVerificationRequestItem second = item(
                100L,
                LATEST_REQUESTED_AT.minusSeconds(60),
                VerificationRequestStatus.PENDING
        );
        AdminVerificationRequestItem extra = item(
                99L,
                LATEST_REQUESTED_AT.minusSeconds(120),
                VerificationRequestStatus.PENDING
        );
        given(queryRepository.findAll(
                VerificationRequestStatus.PENDING,
                null,
                3
        )).willReturn(List.of(first, second, extra));

        AdminVerificationRequestFindAllResponse response = service.findAll(
                UserRole.ADMIN,
                request
        );

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().getFirst().requestId()).isEqualTo(101L);
        assertThat(response.nextCursor()).isEqualTo(cursorCodec.encode(second.toCursor()));
        verify(queryRepository).findAll(VerificationRequestStatus.PENDING, null, 3);
    }

    @Test
    void 상태_크기_커서를_조회_조건으로_전달한다() {
        AdminVerificationRequestCursor cursor = new AdminVerificationRequestCursor(
                LATEST_REQUESTED_AT,
                101L
        );
        String encodedCursor = cursorCodec.encode(cursor);
        AdminVerificationRequestFindAllRequest request =
                new AdminVerificationRequestFindAllRequest("REJECTED", 1, encodedCursor);
        AdminVerificationRequestItem item = item(
                100L,
                LATEST_REQUESTED_AT.minusSeconds(60),
                VerificationRequestStatus.REJECTED
        );
        given(queryRepository.findAll(VerificationRequestStatus.REJECTED, cursor, 2))
                .willReturn(List.of(item));

        AdminVerificationRequestFindAllResponse response = service.findAll(
                UserRole.ADMIN,
                request
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().status()).isEqualTo(VerificationRequestStatus.REJECTED);
        assertThat(response.nextCursor()).isNull();
        verify(queryRepository).findAll(VerificationRequestStatus.REJECTED, cursor, 2);
    }

    @Test
    void 일반_사용자는_관리자_인증_신청_목록을_조회할_수_없다() {
        AdminVerificationRequestFindAllRequest request =
                new AdminVerificationRequestFindAllRequest(null, null, null);

        assertThatThrownBy(() -> service.findAll(UserRole.USER, request))
                .isInstanceOfSatisfying(
                        ForbiddenException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(UserVerificationErrorCode.VERIFICATION_ADMIN_FORBIDDEN)
                );

        verifyNoInteractions(queryRepository);
    }

    @Test
    void 잘못된_커서는_조회하지_않고_거절한다() {
        AdminVerificationRequestFindAllRequest request =
                new AdminVerificationRequestFindAllRequest(null, null, "invalid-cursor");

        assertThatThrownBy(() -> service.findAll(UserRole.ADMIN, request))
                .isInstanceOfSatisfying(
                        BadRequestException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(UserVerificationErrorCode.VERIFICATION_ADMIN_CURSOR_INVALID)
                );

        verifyNoInteractions(queryRepository);
    }

    private AdminVerificationRequestItem item(
            long requestId,
            Instant requestedAt,
            VerificationRequestStatus status
    ) {
        return new AdminVerificationRequestItem(
                requestId,
                42L,
                "charles",
                UserType.WOOWACOURSE_CREW,
                "샤를",
                8,
                "BACKEND",
                status,
                requestedAt
        );
    }
}
