package com.dropit.backend.reaction;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppLikeRepository extends JpaRepository<AppLikeEntity, AppRelationId> {

    @Query("select l.id.appId from AppLikeEntity l where l.id.userId = :userId order by l.createdAt desc")
    List<String> findAppIdsByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query(value = """
        insert into public.app_likes (user_id, app_id, created_at)
        values (:userId, :appId, now())
        on conflict (user_id, app_id) do nothing
        """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId, @Param("appId") String appId);

    @Modifying
    @Query("delete from AppLikeEntity l where l.id.userId = :userId and l.id.appId = :appId")
    int deleteRelation(@Param("userId") UUID userId, @Param("appId") String appId);
}
