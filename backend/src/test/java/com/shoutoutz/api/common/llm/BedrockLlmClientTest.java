package com.shoutoutz.api.common.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.StopReason;
import software.amazon.awssdk.services.bedrockruntime.model.ThrottlingException;
import software.amazon.awssdk.services.bedrockruntime.model.TokenUsage;
import software.amazon.awssdk.services.bedrockruntime.model.ToolChoice;
import software.amazon.awssdk.services.bedrockruntime.model.ToolUseBlock;
import tools.jackson.databind.ObjectMapper;

class BedrockLlmClientTest {

    private static final String MODEL = "global.openai.gpt-6.1-sol";
    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of("questions", Map.of("type", "array", "minItems", 1, "maxItems", 1)),
            "required", List.of("questions"), "additionalProperties", false);
    private static final LlmRequest REQUEST = new LlmRequest("private prompt", SCHEMA, 4_096);

    private final BedrockRuntimeClient bedrock = mock(BedrockRuntimeClient.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final BedrockLlmClient client =
            new BedrockLlmClient(bedrock, LlmProvider.BEDROCK_GPT, MODEL, new ObjectMapper(), registry);

    @Test
    void 스키마를_입력으로_받는_도구를_강제로_호출하고_도구_입력을_JSON으로_돌려준다() {
        when(bedrock.converse(any(ConverseRequest.class))).thenReturn(response(StopReason.TOOL_USE,
                ContentBlock.fromToolUse(ToolUseBlock.builder().toolUseId("t1").name(BedrockLlmClient.TOOL_NAME)
                        .input(BedrockLlmClient.toDocument(Map.of("questions",
                                List.of(Map.of("title", "제목", "content", "본문"))))).build())));

        ObjectMapper mapper = new ObjectMapper();
        assertThat(mapper.readTree(client.generate(REQUEST)))
                .isEqualTo(mapper.readTree("{\"questions\":[{\"title\":\"제목\",\"content\":\"본문\"}]}"));

        ArgumentCaptor<ConverseRequest> captor = ArgumentCaptor.forClass(ConverseRequest.class);
        verify(bedrock).converse(captor.capture());
        ConverseRequest request = captor.getValue();
        assertThat(request.modelId()).isEqualTo(MODEL);
        assertThat(request.messages().getFirst().content().getFirst().text()).isEqualTo("private prompt");
        assertThat(request.inferenceConfig().maxTokens()).isEqualTo(4_096);
        assertThat(request.toolConfig().toolChoice().tool().name()).isEqualTo(BedrockLlmClient.TOOL_NAME);
        Document schema = request.toolConfig().tools().getFirst().toolSpec().inputSchema().json();
        assertThat(BedrockLlmClient.toPlain(schema)).isEqualTo(Map.of(
                "type", "object",
                "properties", Map.of("questions", Map.of("type", "array",
                        "minItems", new java.math.BigDecimal("1"), "maxItems", new java.math.BigDecimal("1"))),
                "required", List.of("questions"), "additionalProperties", false));
    }

    @Test
    void 도구_대신_본문으로_답하면_본문을_그대로_넘긴다() {
        when(bedrock.converse(any(ConverseRequest.class)))
                .thenReturn(response(StopReason.END_TURN, ContentBlock.fromText("{\"questions\":[]}")));

        assertThat(client.generate(REQUEST)).isEqualTo("{\"questions\":[]}");
    }

    @Test
    void Claude는_도구를_강제하지_않고_자동_선택으로_보내며_본문_JSON을_그대로_넘긴다() {
        BedrockLlmClient claude = new BedrockLlmClient(bedrock, LlmProvider.BEDROCK_CLAUDE,
                "global.anthropic.claude-sonnet-5-5", new ObjectMapper(), registry);
        when(bedrock.converse(any(ConverseRequest.class)))
                .thenReturn(response(StopReason.END_TURN, ContentBlock.fromText("{\"questions\":[]}")));

        assertThat(claude.generate(REQUEST)).isEqualTo("{\"questions\":[]}");

        ArgumentCaptor<ConverseRequest> captor = ArgumentCaptor.forClass(ConverseRequest.class);
        verify(bedrock).converse(captor.capture());
        ToolChoice toolChoice = captor.getValue().toolConfig().toolChoice();
        assertThat(toolChoice.auto()).isNotNull();
        assertThat(toolChoice.tool()).isNull();
        assertThat(captor.getValue().toolConfig().tools().getFirst().toolSpec().name())
                .isEqualTo(BedrockLlmClient.TOOL_NAME);
    }

    @Test
    void 출력_한도는_모델_최솟값_아래로_내려가지_않는다() {
        when(bedrock.converse(any(ConverseRequest.class)))
                .thenReturn(response(StopReason.END_TURN, ContentBlock.fromText("{}")));

        client.generate(new LlmRequest("prompt", SCHEMA, 1));

        ArgumentCaptor<ConverseRequest> captor = ArgumentCaptor.forClass(ConverseRequest.class);
        verify(bedrock).converse(captor.capture());
        assertThat(captor.getValue().inferenceConfig().maxTokens()).isEqualTo(BedrockLlmClient.MIN_OUTPUT_TOKENS);
    }

    @Test
    void 호출마다_모델별_입력_출력_토큰을_지표로_쌓는다() {
        when(bedrock.converse(any(ConverseRequest.class)))
                .thenReturn(response(StopReason.END_TURN, ContentBlock.fromText("{}")));

        client.generate(REQUEST);
        client.generate(REQUEST);

        assertThat(registry.get("llm.tokens").tags("provider", "BEDROCK_GPT", "model", MODEL, "type", "input")
                .counter().count()).isEqualTo(200);
        assertThat(registry.get("llm.tokens").tags("provider", "BEDROCK_GPT", "model", MODEL, "type", "output")
                .counter().count()).isEqualTo(60);
    }

    @Test
    void 출력_한도에서_끊기면_재시도_가능한_실패다() {
        when(bedrock.converse(any(ConverseRequest.class)))
                .thenReturn(response(StopReason.MAX_TOKENS, ContentBlock.fromText("{\"questions\":")));

        LlmException failure = catchThrowableOfType(() -> client.generate(REQUEST), LlmException.class);
        assertThat(failure.errorCode()).isEqualTo("INCOMPLETE_RESPONSE");
        assertThat(failure.retryable()).isTrue();
    }

    @Test
    void 콘텐츠_필터에_막히면_재시도하지_않는다() {
        when(bedrock.converse(any(ConverseRequest.class)))
                .thenReturn(response(StopReason.CONTENT_FILTERED, ContentBlock.fromText("")));

        LlmException failure = catchThrowableOfType(() -> client.generate(REQUEST), LlmException.class);
        assertThat(failure.errorCode()).isEqualTo("CONTENT_BLOCKED");
        assertThat(failure.retryable()).isFalse();
    }

    @Test
    void 빈_응답은_재시도_가능한_실패다() {
        when(bedrock.converse(any(ConverseRequest.class)))
                .thenReturn(response(StopReason.END_TURN, ContentBlock.fromText(" ")));

        LlmException failure = catchThrowableOfType(() -> client.generate(REQUEST), LlmException.class);
        assertThat(failure.errorCode()).isEqualTo("EMPTY_RESPONSE");
        assertThat(failure.retryable()).isTrue();
    }

    @Test
    void 호출량_제한은_재시도하고_권한_거부는_재시도하지_않는다() {
        when(bedrock.converse(any(ConverseRequest.class))).thenThrow((Throwable) ThrottlingException.builder()
                .statusCode(429).awsErrorDetails(AwsErrorDetails.builder().errorCode("ThrottlingException").build())
                .build());
        LlmException throttled = catchThrowableOfType(() -> client.generate(REQUEST), LlmException.class);
        assertThat(throttled.provider()).isEqualTo(LlmProvider.BEDROCK_GPT);
        assertThat(throttled.model()).isEqualTo(MODEL);
        assertThat(throttled.httpStatus()).isEqualTo(429);
        assertThat(throttled.errorCode()).isEqualTo("ThrottlingException");
        assertThat(throttled.retryable()).isTrue();

        when(bedrock.converse(any(ConverseRequest.class))).thenThrow((Throwable) AccessDeniedException.builder()
                .statusCode(403).awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDeniedException").build())
                .build());
        LlmException denied = catchThrowableOfType(() -> client.generate(REQUEST), LlmException.class);
        assertThat(denied.errorCode()).isEqualTo("AccessDeniedException");
        assertThat(denied.retryable()).isFalse();
    }

    @Test
    void 연결_오류는_원본_예외를_남기지_않고_재시도_가능한_실패로_바꾼다() {
        when(bedrock.converse(any(ConverseRequest.class)))
                .thenThrow(SdkClientException.create("private network detail"));

        LlmException failure = catchThrowableOfType(() -> client.generate(REQUEST), LlmException.class);
        assertThat(failure.errorCode()).isEqualTo("TRANSPORT_ERROR");
        assertThat(failure.retryable()).isTrue();
        assertThat(failure.getCause()).isNull();
        assertThat(failure.getMessage()).doesNotContain("private network detail");
    }

    private ConverseResponse response(StopReason stopReason, ContentBlock content) {
        return ConverseResponse.builder()
                .stopReason(stopReason)
                .usage(TokenUsage.builder().inputTokens(100).outputTokens(30).totalTokens(130).build())
                .output(ConverseOutput.fromMessage(Message.builder().role(ConversationRole.ASSISTANT)
                        .content(content).build()))
                .build();
    }
}
