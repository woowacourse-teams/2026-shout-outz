package com.shoutoutz.api.interview.application;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.interview.domain.Interview;
import com.shoutoutz.api.interview.domain.InterviewErrorCode;
import com.shoutoutz.api.interview.domain.InterviewRepository;
import com.shoutoutz.api.interview.domain.InterviewStatus;
import com.shoutoutz.api.interview.presentation.dto.request.InterviewSaveRequest;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewService {
    private final InterviewRepository repository;
    private final InterviewAccessService accessService;
    private final InterviewMediaService mediaService;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final Clock clock;

    @Transactional
    public Interview create(long editorId, InterviewSaveRequest request) {
        accessService.requireAdmin(editorId);
        validateInterviewee(request.intervieweeId());
        mediaService.validate(null, request.thumbnailMediaId(), request.contentMd(), editorId);
        Interview interview = Interview.builder().title(request.title().strip()).summary(request.summary())
                .intervieweeId(request.intervieweeId()).thumbnailMediaId(request.thumbnailMediaId())
                .contentMd(request.contentMd()).authorId(editorId).status(InterviewStatus.DRAFT).build();
        return repository.save(interview);
    }

    @Transactional
    public Interview update(long id, long editorId, InterviewSaveRequest request) {
        accessService.requireAdmin(editorId);
        Interview interview = accessService.lockActive(id);
        validateInterviewee(request.intervieweeId());
        String contentMd = mediaService.normalizeContent(interview, request.contentMd());
        mediaService.validate(interview, request.thumbnailMediaId(), contentMd, editorId);
        interview.edit(request.title().strip(), request.summary(), request.intervieweeId(),
                request.thumbnailMediaId(), contentMd);
        return repository.save(interview);
    }

    @Transactional
    public Interview publish(long id, long editorId) {
        accessService.requireAdmin(editorId);
        Interview interview = accessService.lockActive(id);
        validateInterviewee(interview.getIntervieweeId());
        mediaService.validate(interview, interview.getThumbnailMediaId(), interview.getContentMd(), editorId);
        interview.publish(clock.instant());
        return repository.save(interview);
    }

    @Transactional
    public Interview unpublish(long id, long editorId) {
        accessService.requireAdmin(editorId);
        Interview interview = accessService.lockActive(id);
        interview.unpublish();
        return repository.save(interview);
    }

    @Transactional
    public void delete(long id, long editorId) {
        accessService.requireAdmin(editorId);
        Interview interview = accessService.lockActive(id);
        interview.delete(clock.instant());
        repository.save(interview);
    }

    private void validateInterviewee(long userId) {
        userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException(InterviewErrorCode.INTERVIEWEE_NOT_FOUND));
        profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(InterviewErrorCode.INTERVIEWEE_NOT_FOUND));
    }
}
