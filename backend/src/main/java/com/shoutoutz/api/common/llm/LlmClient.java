package com.shoutoutz.api.common.llm;

public interface LlmClient {
    LlmProvider provider();

    String generate(LlmRequest request);
}
