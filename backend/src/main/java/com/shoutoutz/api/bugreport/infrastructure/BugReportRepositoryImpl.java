package com.shoutoutz.api.bugreport.infrastructure;

import com.shoutoutz.api.bugreport.domain.BugReport;
import com.shoutoutz.api.bugreport.domain.BugReportRepository;
import com.shoutoutz.api.bugreport.infrastructure.jpa.BugReportJpaRepository;
import com.shoutoutz.api.bugreport.infrastructure.mapper.BugReportMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BugReportRepositoryImpl implements BugReportRepository {

    private final BugReportJpaRepository bugReportJpaRepository;

    @Override
    public BugReport save(BugReport bugReport) {
        return BugReportMapper.toDomain(
                bugReportJpaRepository.saveAndFlush(BugReportMapper.toEntity(bugReport))
        );
    }

    @Override
    public Optional<BugReport> findById(long bugReportId) {
        return bugReportJpaRepository.findById(bugReportId)
                .map(BugReportMapper::toDomain);
    }
}
