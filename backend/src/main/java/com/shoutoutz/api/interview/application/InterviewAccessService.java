package com.shoutoutz.api.interview.application;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.interview.domain.Interview;
import com.shoutoutz.api.interview.domain.InterviewErrorCode;
import com.shoutoutz.api.interview.domain.InterviewRepository;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.account.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InterviewAccessService {
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;

    public void requireAdmin(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException(InterviewErrorCode.INTERVIEW_ADMIN_FORBIDDEN));
        if (user.getStatus() != UserStatus.ACTIVE || user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException(InterviewErrorCode.INTERVIEW_ADMIN_FORBIDDEN);
        }
    }

    public void requireActiveUser(long userId) {
        userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException(InterviewErrorCode.INTERVIEW_USER_FORBIDDEN));
    }

    public Interview requirePublished(long id) {
        Interview interview = requireActive(id);
        validatePublished(interview);
        return interview;
    }

    public Interview lockPublished(long id) {
        Interview interview = lockActive(id);
        validatePublished(interview);
        return interview;
    }

    public Interview requireActive(long id) {
        return interviewRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException(InterviewErrorCode.INTERVIEW_NOT_FOUND));
    }

    public Interview lockActive(long id) {
        return interviewRepository.findForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException(InterviewErrorCode.INTERVIEW_NOT_FOUND));
    }

    private void validatePublished(Interview interview) {
        if (!interview.isPublished()) {
            throw new EntityNotFoundException(InterviewErrorCode.INTERVIEW_NOT_FOUND);
        }
    }
}
