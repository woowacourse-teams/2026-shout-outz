package com.shoutoutz.api.common.llm;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.AutoToolChoice;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.ModelErrorException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelNotReadyException;
import software.amazon.awssdk.services.bedrockruntime.model.SpecificToolChoice;
import software.amazon.awssdk.services.bedrockruntime.model.StopReason;
import software.amazon.awssdk.services.bedrockruntime.model.TokenUsage;
import software.amazon.awssdk.services.bedrockruntime.model.Tool;
import software.amazon.awssdk.services.bedrockruntime.model.ToolChoice;
import software.amazon.awssdk.services.bedrockruntime.model.ToolConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.ToolInputSchema;
import software.amazon.awssdk.services.bedrockruntime.model.ToolSpecification;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Amazon Bedrock Converse API 로 생성한다. 인증은 EC2 IAM 역할(기본 자격 증명)을 쓴다.
 * 응답 형식은 스키마를 입력으로 받는 도구 하나를 강제로 호출하게 해서 JSON 으로 받는다.
 * Claude 는 도구 강제 지정을 거부하므로 자동 선택으로 보내고, 본문으로 돌려준 JSON 도 받는다.
 */
public final class BedrockLlmClient implements LlmClient {
    static final String TOOL_NAME = "structured_response";
    // OpenAI 계열 모델은 출력 한도가 16 미만이면 요청을 거부한다.
    static final int MIN_OUTPUT_TOKENS = 16;
    private static final Logger log = LoggerFactory.getLogger(BedrockLlmClient.class);

    private final BedrockRuntimeClient client;
    private final LlmProvider provider;
    private final String model;
    private final ObjectMapper mapper;
    private final MeterRegistry meterRegistry;

    public BedrockLlmClient(BedrockRuntimeClient client, LlmProvider provider, String model, ObjectMapper mapper,
            MeterRegistry meterRegistry) {
        this.client = client;
        this.provider = provider;
        this.model = model;
        this.mapper = mapper;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public LlmProvider provider() {
        return provider;
    }

    @Override
    public String generate(LlmRequest request) {
        ConverseResponse response;
        try {
            response = client.converse(converseRequest(request));
        } catch (BedrockRuntimeException exception) {
            int status = exception.statusCode();
            String code = exception.awsErrorDetails() == null ? null : exception.awsErrorDetails().errorCode();
            boolean retryable = status == 408 || status == 429 || status >= 500
                    || exception instanceof ModelNotReadyException || exception instanceof ModelErrorException;
            throw new LlmException(provider, model, status, code == null ? "HTTP_" + status : code, retryable, 0);
        } catch (SdkException exception) {
            throw failure(null, "TRANSPORT_ERROR", true);
        }
        recordUsage(response);
        StopReason stopReason = response.stopReason();
        if (stopReason == StopReason.GUARDRAIL_INTERVENED || stopReason == StopReason.CONTENT_FILTERED) {
            throw failure(200, "CONTENT_BLOCKED", false);
        }
        if (stopReason == StopReason.MAX_TOKENS) {
            throw failure(200, "INCOMPLETE_RESPONSE", true);
        }
        String text = extract(response);
        if (text.isBlank()) {
            throw failure(200, "EMPTY_RESPONSE", true);
        }
        return text;
    }

    private ConverseRequest converseRequest(LlmRequest request) {
        Tool tool = Tool.fromToolSpec(ToolSpecification.builder()
                .name(TOOL_NAME)
                .description("요청한 JSON 스키마에 맞는 응답을 반환한다.")
                .inputSchema(ToolInputSchema.fromJson(toDocument(request.responseSchema())))
                .build());
        return ConverseRequest.builder()
                .modelId(model)
                .messages(Message.builder().role(ConversationRole.USER)
                        .content(ContentBlock.fromText(request.prompt())).build())
                .inferenceConfig(InferenceConfiguration.builder()
                        .maxTokens(Math.max(MIN_OUTPUT_TOKENS, request.maxOutputTokens())).build())
                .toolConfig(ToolConfiguration.builder()
                        .tools(tool)
                        .toolChoice(toolChoice())
                        .build())
                .build();
    }

    private ToolChoice toolChoice() {
        // Bedrock 의 Claude 는 tool_choice 의 tool, any 를 지원하지 않아 ValidationException 으로 거부한다.
        if (provider == LlmProvider.BEDROCK_CLAUDE) {
            return ToolChoice.fromAuto(AutoToolChoice.builder().build());
        }
        return ToolChoice.fromTool(SpecificToolChoice.builder().name(TOOL_NAME).build());
    }

    private String extract(ConverseResponse response) {
        if (response.output() == null || response.output().message() == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (ContentBlock block : response.output().message().content()) {
            if (block.toolUse() != null && TOOL_NAME.equals(block.toolUse().name())) {
                try {
                    return mapper.writeValueAsString(toPlain(block.toolUse().input()));
                } catch (JacksonException exception) {
                    throw failure(200, "INVALID_RESPONSE", true);
                }
            }
            if (block.text() != null) {
                // 도구를 호출하지 않고 본문으로 JSON 을 돌려준 경우에도 이후 검증에 맡긴다.
                text.append(block.text());
            }
        }
        return text.toString();
    }

    private void recordUsage(ConverseResponse response) {
        TokenUsage usage = response.usage();
        if (usage == null) {
            return;
        }
        int input = usage.inputTokens() == null ? 0 : usage.inputTokens();
        int output = usage.outputTokens() == null ? 0 : usage.outputTokens();
        log.info("Bedrock 호출 완료. provider={}, model={}, inputTokens={}, outputTokens={}, stopReason={}",
                provider, model, input, output, response.stopReasonAsString());
        meterRegistry.counter("llm.tokens", "provider", provider.name(), "model", model, "type", "input")
                .increment(input);
        meterRegistry.counter("llm.tokens", "provider", provider.name(), "model", model, "type", "output")
                .increment(output);
    }

    private LlmException failure(Integer status, String code, boolean retryable) {
        return new LlmException(provider, model, status, code, retryable, 0);
    }

    static Document toDocument(Object value) {
        if (value == null) {
            return Document.fromNull();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Document> converted = new LinkedHashMap<>();
            map.forEach((key, item) -> converted.put(String.valueOf(key), toDocument(item)));
            return Document.fromMap(converted);
        }
        if (value instanceof List<?> list) {
            return Document.fromList(list.stream().map(BedrockLlmClient::toDocument).toList());
        }
        if (value instanceof Boolean bool) {
            return Document.fromBoolean(bool);
        }
        if (value instanceof Number number) {
            return Document.fromNumber(SdkNumber.fromString(number.toString()));
        }
        return Document.fromString(value.toString());
    }

    static Object toPlain(Document document) {
        if (document == null || document.isNull()) {
            return null;
        }
        if (document.isMap()) {
            Map<String, Object> converted = new LinkedHashMap<>();
            document.asMap().forEach((key, item) -> converted.put(key, toPlain(item)));
            return converted;
        }
        if (document.isList()) {
            return document.asList().stream().map(BedrockLlmClient::toPlain).toList();
        }
        if (document.isBoolean()) {
            return document.asBoolean();
        }
        if (document.isNumber()) {
            return document.asNumber().bigDecimalValue();
        }
        return document.asString();
    }
}
