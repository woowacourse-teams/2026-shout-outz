package com.dropit.backend.visit;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SiteVisitorRepository extends JpaRepository<SiteVisitorEntity, VisitorRelationId> {

    @Modifying
    @Query(value = """
        insert into public.site_visitors (visitor_id, visited_on, created_at)
        values (:visitorId, :visitedOn, now())
        on conflict (visitor_id, visited_on) do nothing
        """, nativeQuery = true)
    int insertIfAbsent(@Param("visitorId") String visitorId, @Param("visitedOn") LocalDate visitedOn);

    @Query(value = "select count(*) from public.site_visitors where visited_on = :visitedOn", nativeQuery = true)
    long countForDay(@Param("visitedOn") LocalDate visitedOn);

    @Query(value = "select count(distinct visitor_id) from public.site_visitors", nativeQuery = true)
    long countTotalVisitors();
}
