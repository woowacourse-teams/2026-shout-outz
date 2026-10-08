package com.shoutoutz.api.bugreport.application;

import com.shoutoutz.api.bugreport.application.dto.BugReportCursor;
import com.shoutoutz.api.bugreport.application.dto.BugReportPage;
import com.shoutoutz.api.bugreport.domain.BugReportStatus;

public interface BugReportAdminQueryRepository {

    BugReportPage findAll(BugReportStatus status, BugReportCursor cursor, int size);
}
