package com.shoutoutz.api.verification.application;

import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserErrorCode;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileErrorCode;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.verification.domain.UserVerificationErrorCode;
import com.shoutoutz.api.verification.domain.UserVerificationRequest;
import com.shoutoutz.api.verification.domain.UserVerificationRequestHistory;
import com.shoutoutz.api.verification.domain.UserVerificationRequestHistoryRepository;
import com.shoutoutz.api.verification.domain.UserVerificationRequestRepository;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestApproveResponse;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestDecisionActor;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestRejectResponse;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminVerificationRequestDecisionService {

    private final UserVerificationRequestRepository requestRepository;
    private final UserVerificationRequestHistoryRepository historyRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public AdminVerificationRequestApproveResponse approve(
            long requestId,
            long adminUserId,
            UserRole adminRole
    ) {
        validateAdmin(adminRole);
        UserVerificationRequest request = findRequest(requestId);
        validatePending(request);

        UserProfile profile = userProfileRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        UserProfileErrorCode.USER_PROFILE_NOT_FOUND
                ));
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));

        Instant now = clock.instant();
        UserVerificationRequest approvedRequest = request.approve(now);
        UserProfile approvedProfile = profile.approve(
                request.getUserType(),
                request.getNickname(),
                request.getCohort(),
                request.getTrack()
        );
        requestRepository.save(approvedRequest);
        userProfileRepository.save(approvedProfile);
        historyRepository.save(UserVerificationRequestHistory.decision(
                requestId,
                adminUserId,
                VerificationRequestStatus.APPROVED,
                null,
                now
        ));

        return AdminVerificationRequestApproveResponse.of(
                approvedRequest,
                actorOf(admin)
        );
    }

    @Transactional
    public AdminVerificationRequestRejectResponse reject(
            long requestId,
            long adminUserId,
            UserRole adminRole,
            String reason
    ) {
        validateAdmin(adminRole);
        UserVerificationRequest request = findRequest(requestId);
        validatePending(request);
        User admin = findAdmin(adminUserId);

        Instant now = clock.instant();
        UserVerificationRequest rejectedRequest = request.reject(now);
        requestRepository.save(rejectedRequest);
        historyRepository.save(UserVerificationRequestHistory.decision(
                requestId,
                adminUserId,
                VerificationRequestStatus.REJECTED,
                reason,
                now
        ));

        return AdminVerificationRequestRejectResponse.of(
                rejectedRequest,
                reason,
                actorOf(admin)
        );
    }

    private UserVerificationRequest findRequest(long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException(
                        UserVerificationErrorCode.VERIFICATION_REQUEST_NOT_FOUND
                ));
    }

    private User findAdmin(long adminUserId) {
        return userRepository.findById(adminUserId)
                .orElseThrow(() -> new EntityNotFoundException(UserErrorCode.USER_NOT_FOUND));
    }

    private AdminVerificationRequestDecisionActor actorOf(User admin) {
        return new AdminVerificationRequestDecisionActor(
                admin.getId(),
                admin.getHandle().value()
        );
    }

    private void validatePending(UserVerificationRequest request) {
        if (request.getStatus() != VerificationRequestStatus.PENDING) {
            throw new ConflictException(UserVerificationErrorCode.VERIFICATION_REQUEST_NOT_PENDING);
        }
    }

    private void validateAdmin(UserRole role) {
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException(UserVerificationErrorCode.VERIFICATION_ADMIN_FORBIDDEN);
        }
    }
}
