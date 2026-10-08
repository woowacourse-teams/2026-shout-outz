package com.shoutoutz.api.bugreport.infrastructure.jpa;

import com.shoutoutz.api.bugreport.infrastructure.BugReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BugReportJpaRepository extends JpaRepository<BugReportEntity, Long> {
}
