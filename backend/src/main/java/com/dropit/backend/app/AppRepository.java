package com.dropit.backend.app;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppRepository extends JpaRepository<AppEntity, String> {

    @Query(value = """
        select * from public.apps
        where deleted_at is null
          and (cast(:category as text) is null or cast(:category as text) = any(categories))
          and (cast(:q as text) is null
            or lower(name) like lower(concat('%', cast(:q as text), '%'))
            or lower(tagline) like lower(concat('%', cast(:q as text), '%'))
            or lower(cast(tech_tags as text)) like lower(concat('%', cast(:q as text), '%')))
          and (cast(:makerId as uuid) is null or owner_id = cast(:makerId as uuid))
        order by likes desc, created_at desc, id asc
        """, countQuery = """
        select count(*) from public.apps
        where deleted_at is null
          and (cast(:category as text) is null or cast(:category as text) = any(categories))
          and (cast(:q as text) is null
            or lower(name) like lower(concat('%', cast(:q as text), '%'))
            or lower(tagline) like lower(concat('%', cast(:q as text), '%'))
            or lower(cast(tech_tags as text)) like lower(concat('%', cast(:q as text), '%')))
          and (cast(:makerId as uuid) is null or owner_id = cast(:makerId as uuid))
        """, nativeQuery = true)
    Page<AppEntity> findPopular(
        @Param("category") String category,
        @Param("q") String query,
        @Param("makerId") UUID makerId,
        Pageable pageable
    );

    @Query(value = """
        select * from public.apps
        where deleted_at is null
          and (cast(:category as text) is null or cast(:category as text) = any(categories))
          and (cast(:q as text) is null
            or lower(name) like lower(concat('%', cast(:q as text), '%'))
            or lower(tagline) like lower(concat('%', cast(:q as text), '%'))
            or lower(cast(tech_tags as text)) like lower(concat('%', cast(:q as text), '%')))
          and (cast(:makerId as uuid) is null or owner_id = cast(:makerId as uuid))
        order by created_at desc, id asc
        """, countQuery = """
        select count(*) from public.apps
        where deleted_at is null
          and (cast(:category as text) is null or cast(:category as text) = any(categories))
          and (cast(:q as text) is null
            or lower(name) like lower(concat('%', cast(:q as text), '%'))
            or lower(tagline) like lower(concat('%', cast(:q as text), '%'))
            or lower(cast(tech_tags as text)) like lower(concat('%', cast(:q as text), '%')))
          and (cast(:makerId as uuid) is null or owner_id = cast(:makerId as uuid))
        """, nativeQuery = true)
    Page<AppEntity> findLatest(
        @Param("category") String category,
        @Param("q") String query,
        @Param("makerId") UUID makerId,
        Pageable pageable
    );

    Page<AppEntity> findByOwnerIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(UUID ownerId, Pageable pageable);

    @Modifying
    @Query(value = "update public.apps set plays = plays + 1 where id = :appId and deleted_at is null", nativeQuery = true)
    int incrementPlays(@Param("appId") String appId);

    @Modifying
    @Query(value = "update public.apps set likes = likes + 1 where id = :appId and deleted_at is null", nativeQuery = true)
    int incrementLikes(@Param("appId") String appId);

    @Modifying
    @Query(value = "update public.apps set likes = greatest(likes - 1, 0) where id = :appId and deleted_at is null", nativeQuery = true)
    int decrementLikes(@Param("appId") String appId);
}
