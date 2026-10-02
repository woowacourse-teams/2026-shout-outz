package com.shoutoutz.api.verification.application;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserErrorCode;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.verification.domain.UserVerificationErrorCode;
import com.shoutoutz.api.verification.domain.UserVerificationRequestHistory;
import com.shoutoutz.api.verification.domain.UserVerificationRequestHistoryRepository;
import com.shoutoutz.api.verification.domain.UserVerificationRequestRepository;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestDecisionActor;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminVerificationRequestHistoryService {

    private final UserVerificationRequestRepository requestRepository;
    private final UserVerificationRequestHistoryRepository historyRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AdminVerificationRequestHistoryResponse findHistory(
            long requestId,
            UserRole role
    ) {
        validateAdmin(role);
        findRequest(requestId);

        return new AdminVerificationRequestHistoryResponse(
                requestId,
                historyRepository.findAllByRequestId(requestId).stream()
                        .map(this::toItem)
                        .toList()
        );
    }

    private void findRequest(long requestId) {
        requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException(
                        UserVerificationErrorCode.VERIFICATION_REQUEST_NOT_FOUND
                ));
    }

    private AdminVerificationRequestHistoryResponse.Item toItem(
            UserVerificationRequestHistory history
    ) {
        return new AdminVerificationRequestHistoryResponse.Item(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                actorOf(history.getChangedBy()),
                history.getReason(),
                history.getChangedAt()
        );
    }

    private AdminVerificationRequestDecisionActor actorOf(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));
        return new AdminVerificationRequestDecisionActor(
                user.getId(),
                user.getHandle().value()
        );
    }

    private void validateAdmin(UserRole role) {
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException(UserVerificationErrorCode.VERIFICATION_ADMIN_FORBIDDEN);
        }
    }
}
