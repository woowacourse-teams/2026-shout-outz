package com.shoutoutz.api.jobposting.application;

import com.shoutoutz.api.jobposting.application.dto.ExtractedJobPosting;

/**
 * 채용공고 원문에서 공고 정보와 역량 항목을 뽑는다.
 * 뽑은 값은 운영자가 확인하고 고칠 초안이라, 공고에 없는 값은 null 이나 빈 목록으로 돌려준다.
 */
public interface JobPostingExtractor {

    ExtractedJobPosting extract(String content);
}
