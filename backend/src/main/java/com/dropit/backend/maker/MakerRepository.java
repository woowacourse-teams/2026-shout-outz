package com.dropit.backend.maker;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MakerRepository extends JpaRepository<MakerEntity, UUID> {
}
