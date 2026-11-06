package com.shoutoutz.api.jobposting.application;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.jobposting.application.dto.JobPostingAdminCursor;
import com.shoutoutz.api.jobposting.domain.JobPostingErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class JobPostingAdminCursorCodec {

    String encode(JobPostingAdminCursor cursor) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(String.valueOf(cursor.jobPostingId()).getBytes(StandardCharsets.UTF_8));
    }

    JobPostingAdminCursor decode(String encodedCursor) {
        if (encodedCursor == null) {
            return null;
        }
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(encodedCursor),
                    StandardCharsets.UTF_8
            );
            long jobPostingId = Long.parseLong(decoded);
            if (jobPostingId < 1) {
                throw new IllegalArgumentException();
            }
            return new JobPostingAdminCursor(jobPostingId);
        } catch (RuntimeException exception) {
            throw new BadRequestException(
                    JobPostingErrorCode.JOB_POSTING_ADMIN_CURSOR_INVALID,
                    exception
            );
        }
    }
}
