package com.shoutoutz.api.interview.infrastructure.jpa;

import com.shoutoutz.api.interview.infrastructure.InterviewEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InterviewJpaRepository extends JpaRepository<InterviewEntity, Long> {
    Optional<InterviewEntity> findByIdAndDeletedAtIsNull(long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InterviewEntity i where i.id = :id and i.deletedAt is null")
    Optional<InterviewEntity> findForUpdate(long id);
}
