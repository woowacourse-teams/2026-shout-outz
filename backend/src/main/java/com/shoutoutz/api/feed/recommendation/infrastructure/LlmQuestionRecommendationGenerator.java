package com.shoutoutz.api.feed.recommendation.infrastructure;

import com.shoutoutz.api.common.llm.LlmClient;
import com.shoutoutz.api.common.llm.LlmException;
import com.shoutoutz.api.common.llm.LlmProvider;
import com.shoutoutz.api.common.llm.LlmRequest;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationGenerationException;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationGenerator;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationSkippedException;
import com.shoutoutz.api.feed.recommendation.application.dto.GeneratedQuestion;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.CandidateVerdict;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.JudgeResult;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.QuestionRecommendationPrompts;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.RecommendationAngle;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.RecommendationCandidate;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.RecommendationCheck;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.RecommendationJudge;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.RecommendationRuleValidator;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.RecommendationSelector;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.RuleValidationResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 후보 생성 → 규칙 검증 → 교차 judge → 선발 순서로 추천 질문을 만든다.
 * 생성 단계만 Gemini 실패 시 OpenAI로 대체하고, judge는 생성과 다른 제공자로 한 번만 호출한다.
 */
@Component
public class LlmQuestionRecommendationGenerator implements QuestionRecommendationGenerator {

    private static final Logger log = LoggerFactory.getLogger(LlmQuestionRecommendationGenerator.class);
    private static final int MAX_OUTPUT_TOKENS = 8_192;
    private static final Set<String> SKIP_REASONS = Set.of("OFF_TOPIC", "TOO_VAGUE", "INAPPROPRIATE");

    private final Map<LlmProvider, LlmClient> clients;
    private final QuestionRecommendationProperties properties;
    private final QuestionRecommendationQualityProperties quality;
    private final QuestionRecommendationPrompts prompts;
    private final RecommendationJudge judge;
    private final RecommendationRuleValidator ruleValidator;
    private final RecommendationSelector selector;
    private final ObjectMapper objectMapper;

    public LlmQuestionRecommendationGenerator(
            List<LlmClient> clients,
            QuestionRecommendationProperties properties,
            QuestionRecommendationQualityProperties quality,
            QuestionRecommendationPrompts prompts,
            RecommendationJudge judge,
            ObjectMapper objectMapper
    ) {
        this.clients = clients.stream().collect(Collectors.toMap(LlmClient::provider, Function.identity()));
        this.properties = properties;
        this.quality = quality;
        this.prompts = prompts;
        this.judge = judge;
        this.ruleValidator = new RecommendationRuleValidator(quality.rules());
        this.selector = new RecommendationSelector(quality.rules().maxCandidateTitleSimilarity());
        this.objectMapper = objectMapper;
    }

    @Override
    public List<GeneratedQuestion> generate(String title, String content, int count) {
        String source = sourceJson(title, content);
        Generation generation = generateCandidates(source);
        RuleValidationResult validated = ruleValidator.validate(title, generation.candidates());
        if (validated.passed().size() < quality.minCount()) {
            log.warn("질문 추천 규칙 검증 통과 후보 부족. provider={}, candidates={}, passed={}, violations={}",
                    generation.provider(), generation.candidates().size(), validated.passed().size(),
                    violationCounts(validated));
            throw new QuestionRecommendationGenerationException(true, 0);
        }
        JudgeResult judgement = judge.judge(generation.provider(), source, validated.passed());
        List<RecommendationCandidate> selected = selector.select(validated.passed(), judgement, count);
        log.info("질문 추천 품질 검증 완료. provider={}, candidates={}, rulePassed={}, violations={}, "
                        + "hardGateFailures={}, duplicateGroups={}, selected={}",
                generation.provider(), generation.candidates().size(), validated.passed().size(),
                violationCounts(validated), hardGateFailures(judgement), judgement.duplicateGroups().size(),
                selected.size());
        if (selected.size() < quality.minCount()) {
            throw new QuestionRecommendationGenerationException(true, 0);
        }
        return selected.stream().map(RecommendationCandidate::toGeneratedQuestion).toList();
    }

    private Generation generateCandidates(String source) {
        LlmRequest request = new LlmRequest(
                prompts.generation(source, quality.candidateCount(), quality.minAngleKinds()),
                schema(), MAX_OUTPUT_TOKENS);
        List<LlmProvider> providers = properties.fallbackEnabled()
                ? List.of(LlmProvider.GEMINI, LlmProvider.OPENAI) : List.of(LlmProvider.GEMINI);
        boolean retryable = false;
        long retryAfterSeconds = 0;
        for (LlmProvider provider : providers) {
            boolean fallback = provider != LlmProvider.GEMINI;
            try {
                List<RecommendationCandidate> candidates = decode(clients.get(provider).generate(request));
                log.info("질문 추천 후보 생성 완료. provider={}, fallback={}, candidates={}",
                        provider, fallback, candidates.size());
                return new Generation(provider, candidates);
            } catch (LlmException exception) {
                log.warn("질문 추천 제공자 호출 실패. provider={}, model={}, httpStatus={}, errorCode={}, "
                                + "retryAfterSeconds={}, fallback={}",
                        provider, exception.model(), exception.httpStatus(), exception.errorCode(),
                        exception.retryAfterSeconds(), fallback);
                retryable |= exception.retryable();
                if (exception.retryable()) {
                    retryAfterSeconds = Math.max(retryAfterSeconds, exception.retryAfterSeconds());
                }
            } catch (InvalidRecommendationException exception) {
                log.warn("질문 추천 응답 검증 실패. provider={}, errorCode=INVALID_RECOMMENDATIONS, fallback={}",
                        provider, fallback);
                retryable = true;
            }
        }
        throw new QuestionRecommendationGenerationException(retryable, retryAfterSeconds);
    }

    private String sourceJson(String title, String content) {
        try {
            Map<String, String> source = new LinkedHashMap<>();
            source.put("title", title);
            source.put("content", content);
            return objectMapper.writeValueAsString(source);
        } catch (JacksonException exception) {
            throw new QuestionRecommendationGenerationException(false, 0);
        }
    }

    private Map<String, Object> schema() {
        List<String> angles = Arrays.stream(RecommendationAngle.values()).map(Enum::name).toList();
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "decision", Map.of("type", "string", "enum", List.of("GENERATE", "SKIP")),
                        "skipReason", Map.of("type", "string",
                                "enum", List.of("NONE", "OFF_TOPIC", "TOO_VAGUE", "INAPPROPRIATE")),
                        "questions", Map.of(
                                "type", "array", "minItems", 0, "maxItems", quality.candidateCount(),
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "angle", Map.of("type", "string", "enum", angles),
                                                "title", Map.of("type", "string"),
                                                "content", Map.of("type", "string")),
                                        "required", List.of("angle", "title", "content"),
                                        "additionalProperties", false))),
                "required", List.of("decision", "skipReason", "questions"), "additionalProperties", false
        );
    }

    private List<RecommendationCandidate> decode(String generatedJson) {
        if (generatedJson == null || generatedJson.isBlank()) {
            throw new InvalidRecommendationException();
        }
        GenerationPayload payload;
        try {
            payload = objectMapper.readValue(generatedJson, GenerationPayload.class);
        } catch (JacksonException exception) {
            throw new InvalidRecommendationException();
        }
        if (payload == null || payload.decision() == null) {
            throw new InvalidRecommendationException();
        }
        if ("SKIP".equals(payload.decision())) {
            if (!SKIP_REASONS.contains(payload.skipReason())) {
                throw new InvalidRecommendationException();
            }
            throw new QuestionRecommendationSkippedException(payload.skipReason());
        }
        if (!"GENERATE".equals(payload.decision()) || payload.questions() == null
                || payload.questions().size() < quality.minCount()
                || payload.questions().size() > quality.candidateCount()) {
            throw new InvalidRecommendationException();
        }
        List<RecommendationCandidate> candidates = new ArrayList<>();
        Set<String> distinct = new HashSet<>();
        for (CandidatePayload question : payload.questions()) {
            RecommendationCandidate candidate = normalize(question, candidates.size() + 1);
            if (distinct.add((candidate.title() + "\n" + candidate.content()).toLowerCase(Locale.ROOT))) {
                candidates.add(candidate);
            }
        }
        long angleKinds = candidates.stream().map(RecommendationCandidate::angle).distinct().count();
        if (angleKinds < quality.minAngleKinds()) {
            throw new InvalidRecommendationException();
        }
        return candidates;
    }

    private RecommendationCandidate normalize(CandidatePayload question, int order) {
        if (question == null || question.title() == null || question.content() == null || question.angle() == null) {
            throw new InvalidRecommendationException();
        }
        RecommendationAngle angle;
        try {
            angle = RecommendationAngle.valueOf(question.angle());
        } catch (IllegalArgumentException exception) {
            throw new InvalidRecommendationException();
        }
        String title = question.title().strip();
        String content = question.content().strip();
        if (title.isBlank() || content.isBlank()) {
            throw new InvalidRecommendationException();
        }
        return new RecommendationCandidate("c" + order, order, angle, title, content);
    }

    private Map<String, Long> violationCounts(RuleValidationResult validated) {
        return validated.rejected().values().stream()
                .collect(Collectors.groupingBy(Enum::name, Collectors.counting()));
    }

    private Map<RecommendationCheck, Long> hardGateFailures(JudgeResult judgement) {
        Map<RecommendationCheck, Long> failures = new EnumMap<>(RecommendationCheck.class);
        for (CandidateVerdict verdict : judgement.verdicts()) {
            RecommendationCheck.hardGates().stream()
                    .filter(check -> !verdict.checks().get(check))
                    .forEach(check -> failures.merge(check, 1L, Long::sum));
        }
        return failures;
    }

    private record Generation(LlmProvider provider, List<RecommendationCandidate> candidates) {
    }

    private record GenerationPayload(String decision, String skipReason, List<CandidatePayload> questions) {
    }

    private record CandidatePayload(String angle, String title, String content) {
    }

    private static class InvalidRecommendationException extends RuntimeException {
    }
}
