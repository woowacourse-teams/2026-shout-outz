package com.dropit.backend.crew;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrewMemberRepository extends JpaRepository<CrewMemberEntity, UUID> {

    @Modifying
    @Query(value = """
        insert into public.crew_members (user_id, verified_at)
        values (:userId, now())
        on conflict (user_id) do nothing
        """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId);
}
