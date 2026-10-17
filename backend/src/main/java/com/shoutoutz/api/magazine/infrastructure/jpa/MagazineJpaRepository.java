package com.shoutoutz.api.magazine.infrastructure.jpa;

import com.shoutoutz.api.magazine.infrastructure.MagazineEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MagazineJpaRepository extends JpaRepository<MagazineEntity, Long> {
    Optional<MagazineEntity> findByIdAndDeletedAtIsNull(long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from MagazineEntity i where i.id = :id and i.deletedAt is null")
    Optional<MagazineEntity> findForUpdate(long id);
}
