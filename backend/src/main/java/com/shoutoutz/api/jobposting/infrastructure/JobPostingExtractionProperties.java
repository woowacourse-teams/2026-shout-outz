package com.shoutoutz.api.jobposting.infrastructure;

import com.shoutoutz.api.common.llm.LlmProvider;
import java.util.HashSet;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 앞에서부터 호출하고 실패하면 다음 제공자로 넘어간다. 대체하지 않으려면 제공자를 하나만 둔다.
 */
@ConfigurationProperties(prefix = "job-posting.extraction")
public record JobPostingExtractionProperties(
        @DefaultValue({"BEDROCK_GPT", "BEDROCK_CLAUDE"}) List<LlmProvider> providers
) {

    private static final List<LlmProvider> DEFAULT_PROVIDERS = List.of(LlmProvider.BEDROCK_GPT, LlmProvider.BEDROCK_CLAUDE);

    public JobPostingExtractionProperties {
        providers = providers == null || providers.isEmpty() ? DEFAULT_PROVIDERS : List.copyOf(providers);
        if (new HashSet<>(providers).size() != providers.size()) {
            throw new IllegalArgumentException("job-posting.extraction.providers에 같은 제공자를 두 번 넣을 수 없습니다.");
        }
    }
}
