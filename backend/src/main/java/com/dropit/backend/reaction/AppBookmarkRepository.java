package com.dropit.backend.reaction;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppBookmarkRepository extends JpaRepository<AppBookmarkEntity, AppRelationId> {

    @Query("select b.id.appId from AppBookmarkEntity b where b.id.userId = :userId order by b.createdAt desc")
    List<String> findAppIdsByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query(value = """
        insert into public.app_bookmarks (user_id, app_id, created_at)
        values (:userId, :appId, now())
        on conflict (user_id, app_id) do nothing
        """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId, @Param("appId") String appId);

    @Modifying
    @Query("delete from AppBookmarkEntity b where b.id.userId = :userId and b.id.appId = :appId")
    int deleteRelation(@Param("userId") UUID userId, @Param("appId") String appId);
}
