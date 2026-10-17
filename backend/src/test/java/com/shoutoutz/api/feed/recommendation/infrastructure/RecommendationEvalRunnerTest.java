package com.shoutoutz.api.feed.recommendation.infrastructure;

import static org.assertj.core.api.Assumptions.assumeThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.shoutoutz.api.common.llm.GeminiLlmClient;
import com.shoutoutz.api.common.llm.LlmClient;
import com.shoutoutz.api.common.llm.LlmProperties;
import com.shoutoutz.api.common.llm.OpenAiLlmClient;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationGenerationException;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationSkippedException;
import com.shoutoutz.api.feed.recommendation.application.dto.GeneratedQuestion;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.QuestionRecommendationPrompts;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.RecommendationJudge;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 실제 LLM API로 평가 세트를 돌려 프롬프트 변경의 품질 회귀를 확인한다. 기본 테스트에서는 제외되며
 * GEMINI_API_KEY, OPENAI_API_KEY 환경 변수가 있을 때 `./gradlew test -PincludeTags=llm-eval`로 실행한다.
 * 리포트에는 생성된 질문이 담기므로 build/ 아래(커밋 대상 아님)에만 쓴다.
 */
@Tag("llm-eval")
class RecommendationEvalRunnerTest {

    private static final String CASES = "recommendation-eval/cases.jsonl";
    private static final Path REPORT_DIR = Path.of("build", "reports", "recommendation-eval");

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 평가_세트를_실행하고_리포트를_남긴다() throws IOException {
        String geminiKey = System.getenv("GEMINI_API_KEY");
        String openaiKey = System.getenv("OPENAI_API_KEY");
        assumeThat(geminiKey).as("GEMINI_API_KEY 환경 변수가 필요합니다.").isNotBlank();
        assumeThat(openaiKey).as("OPENAI_API_KEY 환경 변수가 필요합니다.").isNotBlank();

        List<LlmClient> clients = List.of(
                new GeminiLlmClient(client("https://generativelanguage.googleapis.com"),
                        new LlmProperties.Gemini(geminiKey, System.getenv("GEMINI_MODEL"), null, null), mapper),
                new OpenAiLlmClient(client("https://api.openai.com"),
                        new LlmProperties.OpenAi(openaiKey, System.getenv("OPENAI_MODEL"), null, null, null), mapper));
        QuestionRecommendationPrompts prompts = new QuestionRecommendationPrompts();
        LlmQuestionRecommendationGenerator generator = new LlmQuestionRecommendationGenerator(clients,
                new QuestionRecommendationProperties(5, 5, 5, 5000, true, null),
                new QuestionRecommendationQualityProperties(8, 3, 3, null),
                prompts, new RecommendationJudge(clients, prompts, mapper), mapper);

        Logger generatorLogger = (Logger) LoggerFactory.getLogger(LlmQuestionRecommendationGenerator.class);
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        logs.start();
        generatorLogger.addAppender(logs);
        List<String> rows = new ArrayList<>();
        List<String> details = new ArrayList<>();
        int decisionMatches = 0;
        int total = 0;
        try {
            for (JsonNode testCase : cases()) {
                total++;
                logs.list.clear();
                String expected = testCase.path("expected").path("decision").asString();
                String actual;
                String outcome;
                List<GeneratedQuestion> questions = List.of();
                try {
                    questions = generator.generate(testCase.path("title").asString(),
                            testCase.path("content").asString(), 5);
                    actual = "GENERATE";
                    outcome = "READY " + questions.size() + "개";
                } catch (QuestionRecommendationSkippedException exception) {
                    actual = "SKIP";
                    outcome = "SKIPPED " + exception.reason();
                } catch (QuestionRecommendationGenerationException exception) {
                    actual = "GENERATE";
                    outcome = "FAILED retryable=" + exception.retryable();
                }
                if (actual.equals(expected)) {
                    decisionMatches++;
                }
                String quality = logs.list.stream().map(ILoggingEvent::getFormattedMessage)
                        .filter(message -> message.startsWith("질문 추천 품질 검증 완료")
                                || message.startsWith("질문 추천 규칙 검증 통과 후보 부족"))
                        .findFirst().orElse("-");
                rows.add("| %s | %s | %s | %s | %s |".formatted(testCase.path("id").asString(),
                        testCase.path("category").asString(), expected, outcome, quality.replace("|", "/")));
                details.add("### " + testCase.path("id").asString() + " " + testCase.path("title").asString() + "\n"
                        + questions.stream().map(question -> "- **" + question.title() + "**\n  " + question.content())
                        .reduce((left, right) -> left + "\n" + right).orElse("(추천 없음)") + "\n");
            }
        } finally {
            generatorLogger.detachAppender(logs);
        }

        Files.createDirectories(REPORT_DIR);
        Path report = REPORT_DIR.resolve("report-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".md");
        Files.writeString(report, """
                # 질문 추천 평가 리포트

                - SKIP 판정 일치: %d / %d

                | id | 범주 | 기대 | 결과 | 품질 로그 |
                | --- | --- | --- | --- | --- |
                %s

                ## 선발된 질문
                %s
                """.formatted(decisionMatches, total, String.join("\n", rows), String.join("\n", details)),
                StandardCharsets.UTF_8);
        System.out.println("질문 추천 평가 리포트: " + report.toAbsolutePath());
    }

    private List<JsonNode> cases() throws IOException {
        List<JsonNode> cases = new ArrayList<>();
        try (InputStream input = new ClassPathResource(CASES).getInputStream()) {
            for (String line : new String(input.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!line.isBlank()) {
                    cases.add(mapper.readTree(line));
                }
            }
        }
        return cases;
    }

    private RestClient client(String baseUrl) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(90));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
