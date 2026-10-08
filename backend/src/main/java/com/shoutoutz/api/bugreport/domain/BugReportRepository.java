package com.shoutoutz.api.bugreport.domain;

import java.util.Optional;

public interface BugReportRepository {

    BugReport save(BugReport bugReport);

    Optional<BugReport> findById(long bugReportId);
}
