package com.shoutoutz.api.verification.application;

import com.shoutoutz.api.verification.application.dto.AdminVerificationRequestCursor;
import com.shoutoutz.api.verification.application.dto.AdminVerificationRequestItem;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import java.util.List;

public interface AdminVerificationRequestQueryRepository {

    List<AdminVerificationRequestItem> findAll(
            VerificationRequestStatus status,
            AdminVerificationRequestCursor cursor,
            int limit
    );
}
