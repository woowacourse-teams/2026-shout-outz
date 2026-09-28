package com.shoutoutz.api.verification.application;

import com.shoutoutz.api.verification.application.dto.AdminVerificationRequestCursor;
import com.shoutoutz.api.verification.application.dto.AdminVerificationRequestPage;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;

public interface AdminVerificationRequestQueryRepository {

    AdminVerificationRequestPage findAll(
            VerificationRequestStatus status,
            AdminVerificationRequestCursor cursor,
            int size
    );
}
