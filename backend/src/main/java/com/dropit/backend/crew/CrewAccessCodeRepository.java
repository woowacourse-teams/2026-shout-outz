package com.dropit.backend.crew;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CrewAccessCodeRepository extends JpaRepository<CrewAccessCodeEntity, Long> {

    @Query(value = """
        select exists(
            select 1
            from public.crew_access_codes
            where active = true
              and crypt(trim(:code), code_hash) = code_hash
        )
        """, nativeQuery = true)
    boolean existsValidCode(String code);
}
