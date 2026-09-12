package com.shoutoutz.api.visitor.infrastructure.jpa;

import com.shoutoutz.api.visitor.infrastructure.VisitorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitorJpaRepository extends JpaRepository<VisitorEntity, Long> {
}
