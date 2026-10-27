package com.shoutoutz.api.jobposting.infrastructure;

import com.shoutoutz.api.common.llm.LlmProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 공고 추출은 요청 안에서 기다리는 호출이라 제공자 하나만 쓰고 자동 대체하지 않는다.
 */
@ConfigurationProperties(prefix = "job-posting.extraction")
public record JobPostingExtractionProperties(@DefaultValue("BEDROCK_GPT") LlmProvider provider) {

    public JobPostingExtractionProperties {
        provider = provider == null ? LlmProvider.BEDROCK_GPT : provider;
    }
}
