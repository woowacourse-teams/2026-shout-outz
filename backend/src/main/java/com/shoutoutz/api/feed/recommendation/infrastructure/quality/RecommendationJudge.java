package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

import com.shoutoutz.api.common.llm.LlmClient;
import com.shoutoutz.api.common.llm.LlmException;
import com.shoutoutz.api.common.llm.LlmProvider;
import com.shoutoutz.api.common.llm.LlmRequest;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationGenerationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 생성과 다른 제공자로 후보를 한 번 채점한다. 자기 출력을 후하게 보는 편향을 줄이기 위해 교차 채점하고,
 * 시간 예산 안에 들도록 대체 호출은 하지 않는다. 실패하면 검수되지 않은 질문을 노출하지 않도록 재시도로 돌린다.
 */
@Component
public class RecommendationJudge {

    private static final Logger log = LoggerFactory.getLogger(RecommendationJudge.class);
    private static final int MAX_OUTPUT_TOKENS = 8_192;
    private static final double TEMPERATURE = 0.0;

    private final Map<LlmProvider, LlmClient> clients;
    private final QuestionRecommendationPrompts prompts;
    private final ObjectMapper objectMapper;
    private final Random random;

    @Autowired
    public RecommendationJudge(List<LlmClient> clients, QuestionRecommendationPrompts prompts, ObjectMapper objectMapper) {
        this(clients, prompts, objectMapper, new Random());
    }

    RecommendationJudge(List<LlmClient> clients, QuestionRecommendationPrompts prompts, ObjectMapper objectMapper,
            Random random) {
        this.clients = clients.stream().collect(Collectors.toMap(LlmClient::provider, Function.identity()));
        this.prompts = prompts;
        this.objectMapper = objectMapper;
        this.random = random;
    }

    public static LlmProvider judgeProviderFor(LlmProvider generator) {
        return generator == LlmProvider.GEMINI ? LlmProvider.OPENAI : LlmProvider.GEMINI;
    }

    public JudgeResult judge(LlmProvider generator, String sourceJson, List<RecommendationCandidate> candidates) {
        LlmProvider provider = judgeProviderFor(generator);
        LlmRequest request = new LlmRequest(prompts.judge(sourceJson, candidatesJson(candidates)), schema(),
                MAX_OUTPUT_TOKENS, TEMPERATURE);
        String response;
        try {
            response = clients.get(provider).generate(request);
        } catch (LlmException exception) {
            log.warn("질문 추천 judge 호출 실패. provider={}, model={}, httpStatus={}, errorCode={}, retryAfterSeconds={}",
                    provider, exception.model(), exception.httpStatus(), exception.errorCode(),
                    exception.retryAfterSeconds());
            throw new QuestionRecommendationGenerationException(true, exception.retryAfterSeconds());
        }
        JudgeResult result = decode(response, candidates);
        if (result == null) {
            log.warn("질문 추천 judge 응답 검증 실패. provider={}, errorCode=INVALID_JUDGEMENT", provider);
            throw new QuestionRecommendationGenerationException(true, 0);
        }
        return result;
    }

    private String candidatesJson(List<RecommendationCandidate> candidates) {
        List<RecommendationCandidate> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, random);
        List<Map<String, String>> payload = shuffled.stream()
                .map(candidate -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("id", candidate.id());
                    item.put("angle", candidate.angle().name());
                    item.put("title", candidate.title());
                    item.put("content", candidate.content());
                    return item;
                })
                .toList();
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException exception) {
            throw new QuestionRecommendationGenerationException(false, 0);
        }
    }

    private JudgeResult decode(String response, List<RecommendationCandidate> candidates) {
        JsonNode root;
        try {
            root = response == null ? null : objectMapper.readTree(response);
        } catch (JacksonException exception) {
            return null;
        }
        if (root == null || !root.path("results").isArray()) {
            return null;
        }
        Set<String> expectedIds = candidates.stream().map(RecommendationCandidate::id).collect(Collectors.toSet());
        Map<String, CandidateVerdict> verdicts = new LinkedHashMap<>();
        for (JsonNode result : root.path("results")) {
            String id = result.path("id").asString("");
            Map<RecommendationCheck, Boolean> checks = checks(result.path("checks"));
            if (!expectedIds.contains(id) || verdicts.containsKey(id) || checks == null) {
                return null;
            }
            verdicts.put(id, new CandidateVerdict(id, checks));
        }
        if (!verdicts.keySet().equals(expectedIds)) {
            return null;
        }
        List<List<String>> duplicateGroups = new ArrayList<>();
        for (JsonNode group : root.path("duplicateGroups")) {
            List<String> ids = new ArrayList<>();
            for (JsonNode id : group) {
                if (expectedIds.contains(id.asString("")) && !ids.contains(id.asString(""))) {
                    ids.add(id.asString(""));
                }
            }
            if (ids.size() > 1) {
                duplicateGroups.add(ids);
            }
        }
        return new JudgeResult(List.copyOf(verdicts.values()), duplicateGroups);
    }

    private Map<RecommendationCheck, Boolean> checks(JsonNode items) {
        Map<RecommendationCheck, Boolean> checks = new EnumMap<>(RecommendationCheck.class);
        Set<String> names = Arrays.stream(RecommendationCheck.values()).map(Enum::name).collect(Collectors.toSet());
        Set<String> seen = new HashSet<>();
        for (JsonNode item : items) {
            String name = item.path("item").asString("");
            if (!names.contains(name) || !seen.add(name) || !item.path("pass").isBoolean()) {
                return null;
            }
            checks.put(RecommendationCheck.valueOf(name), item.path("pass").asBoolean());
        }
        return checks.size() == RecommendationCheck.values().length ? checks : null;
    }

    private Map<String, Object> schema() {
        List<String> items = Arrays.stream(RecommendationCheck.values()).map(Enum::name).toList();
        // 근거를 먼저 쓰고 판정하도록 why를 pass보다 앞에 둔다.
        Map<String, Object> check = Map.of(
                "type", "object",
                "properties", ordered(
                        "item", Map.of("type", "string", "enum", items),
                        "why", Map.of("type", "string"),
                        "pass", Map.of("type", "boolean")),
                "required", List.of("item", "why", "pass"), "additionalProperties", false);
        Map<String, Object> result = Map.of(
                "type", "object",
                "properties", ordered(
                        "id", Map.of("type", "string"),
                        "checks", Map.of("type", "array", "items", check)),
                "required", List.of("id", "checks"), "additionalProperties", false);
        return Map.of(
                "type", "object",
                "properties", ordered(
                        "results", Map.of("type", "array", "items", result),
                        "duplicateGroups", Map.of("type", "array",
                                "items", Map.of("type", "array", "items", Map.of("type", "string")))),
                "required", List.of("results", "duplicateGroups"), "additionalProperties", false);
    }

    private static Map<String, Object> ordered(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            map.put((String) keyValues[index], keyValues[index + 1]);
        }
        return map;
    }
}
