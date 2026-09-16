package com.shoutoutz.api.verification.application;

import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.verification.application.dto.AdminVerificationRequestCursor;
import com.shoutoutz.api.verification.application.dto.AdminVerificationRequestItem;
import com.shoutoutz.api.verification.domain.UserVerificationErrorCode;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import com.shoutoutz.api.verification.presentation.dto.request.AdminVerificationRequestFindAllRequest;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestFindAllResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminVerificationRequestService {

    private final AdminVerificationRequestQueryRepository queryRepository;
    private final AdminVerificationRequestCursorCodec cursorCodec;

    @Transactional(readOnly = true)
    public AdminVerificationRequestFindAllResponse findAll(
            UserRole role,
            AdminVerificationRequestFindAllRequest request
    ) {
        validateAdmin(role);

        VerificationRequestStatus status = request.resolvedStatus();
        AdminVerificationRequestCursor cursor = cursorCodec.decode(request.cursor());
        int size = request.resolvedSize();
        List<AdminVerificationRequestItem> fetched = queryRepository.findAll(
                status,
                cursor,
                size + 1
        );

        boolean hasNext = fetched.size() > size;
        List<AdminVerificationRequestItem> items = hasNext
                ? List.copyOf(fetched.subList(0, size))
                : List.copyOf(fetched);
        String nextCursor = hasNext ? cursorCodec.encode(items.getLast().toCursor()) : null;
        return AdminVerificationRequestFindAllResponse.from(items, nextCursor);
    }

    private void validateAdmin(UserRole role) {
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException(UserVerificationErrorCode.VERIFICATION_ADMIN_FORBIDDEN);
        }
    }
}
