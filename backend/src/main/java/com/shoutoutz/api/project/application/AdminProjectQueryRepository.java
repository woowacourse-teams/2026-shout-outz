package com.shoutoutz.api.project.application;

import com.shoutoutz.api.project.application.dto.AdminProjectCursor;
import com.shoutoutz.api.project.application.dto.AdminProjectPage;
import com.shoutoutz.api.project.domain.ApprovalStatus;

public interface AdminProjectQueryRepository {

    AdminProjectPage findAll(ApprovalStatus status, AdminProjectCursor cursor, int size);
}
