package com.shoutoutz.api.verification.application;

import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileErrorCode;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.verification.domain.UserVerificationErrorCode;
import com.shoutoutz.api.verification.domain.UserVerificationRequest;
import com.shoutoutz.api.verification.domain.UserVerificationRequestHistory;
import com.shoutoutz.api.verification.domain.UserVerificationRequestHistoryRepository;
import com.shoutoutz.api.verification.domain.UserVerificationRequestRepository;
import com.shoutoutz.api.verification.presentation.dto.request.UserVerificationRequestCreateRequest;
import com.shoutoutz.api.verification.presentation.dto.response.UserVerificationRequestCreateResponse;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserVerificationService {

    private final UserProfileRepository userProfileRepository;
    private final UserVerificationRequestRepository requestRepository;
    private final UserVerificationRequestHistoryRepository historyRepository;
    private final Clock clock;

    @Transactional
    public UserVerificationRequestCreateResponse create(
            long userId,
            UserVerificationRequestCreateRequest request
    ) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        UserProfileErrorCode.USER_PROFILE_NOT_FOUND
                ));
        validateApplicant(profile);
        if (requestRepository.findPendingByUserId(userId).isPresent()) {
            throw new ConflictException(
                    UserVerificationErrorCode.VERIFICATION_REQUEST_ALREADY_PENDING
            );
        }

        Instant now = clock.instant();
        UserVerificationRequest verificationRequest = UserVerificationRequest.create(
                userId,
                request.userType(),
                request.nickname(),
                request.cohort(),
                request.track(),
                now
        );
        UserVerificationRequest savedRequest = requestRepository.save(verificationRequest);
        historyRepository.save(UserVerificationRequestHistory.initial(savedRequest.getId(), now));

        return UserVerificationRequestCreateResponse.from(savedRequest);
    }

    private void validateApplicant(UserProfile profile) {
        if (profile.getUserType() != UserType.GENERAL) {
            throw new ConflictException(UserVerificationErrorCode.VERIFICATION_ALREADY_APPROVED);
        }
    }
}
