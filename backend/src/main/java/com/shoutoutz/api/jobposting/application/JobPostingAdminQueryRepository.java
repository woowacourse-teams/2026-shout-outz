package com.shoutoutz.api.jobposting.application;

import com.shoutoutz.api.jobposting.application.dto.JobPostingCursor;
import com.shoutoutz.api.jobposting.application.dto.JobPostingPage;

public interface JobPostingAdminQueryRepository {

    /**
     * 최신 등록순으로 size 개를 읽는다. cursor 가 null 이면 처음부터 읽는다.
     */
    JobPostingPage findAll(JobPostingCursor cursor, int size);
}
