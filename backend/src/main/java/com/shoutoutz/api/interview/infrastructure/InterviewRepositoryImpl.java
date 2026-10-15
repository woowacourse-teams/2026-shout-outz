package com.shoutoutz.api.interview.infrastructure;

import com.shoutoutz.api.interview.domain.Interview;
import com.shoutoutz.api.interview.domain.InterviewRepository;
import com.shoutoutz.api.interview.infrastructure.jpa.InterviewJpaRepository;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.interview.domain.InterviewErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InterviewRepositoryImpl implements InterviewRepository {
    private final InterviewJpaRepository jpaRepository;

    @Override
    public Interview save(Interview interview) {
        if (interview.getId() == null) {
            return jpaRepository.saveAndFlush(InterviewEntity.from(interview)).toDomain();
        }
        InterviewEntity entity = jpaRepository.findByIdAndDeletedAtIsNull(interview.getId())
                .orElseThrow(() -> new EntityNotFoundException(InterviewErrorCode.INTERVIEW_NOT_FOUND));
        entity.update(interview);
        jpaRepository.flush();
        return entity.toDomain();
    }

    @Override
    public Optional<Interview> findActiveById(long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id).map(InterviewEntity::toDomain);
    }

    @Override
    public Optional<Interview> findForUpdate(long id) {
        return jpaRepository.findForUpdate(id).map(InterviewEntity::toDomain);
    }
}
