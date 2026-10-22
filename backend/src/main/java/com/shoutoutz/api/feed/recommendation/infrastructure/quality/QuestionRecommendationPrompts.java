package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 추천 질문 생성과 judge 프롬프트 템플릿을 기동 시 읽고 렌더링한다.
 * 사용자 입력과 LLM 출력에 {{...}} 문자열이 섞여도 다시 치환되지 않도록 한 번에 치환한다.
 */
@Component
public class QuestionRecommendationPrompts {

    private static final String GENERATE_TEMPLATE = "prompts/question-recommendation/generate.txt";
    private static final String JUDGE_TEMPLATE = "prompts/question-recommendation/judge.txt";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    private final String generateTemplate;
    private final String judgeTemplate;

    public QuestionRecommendationPrompts() {
        this(read(GENERATE_TEMPLATE), read(JUDGE_TEMPLATE));
    }

    QuestionRecommendationPrompts(String generateTemplate, String judgeTemplate) {
        this.generateTemplate = generateTemplate;
        this.judgeTemplate = judgeTemplate;
    }

    public String generation(String sourceJson, int candidateCount, int minAngleKinds) {
        return render(generateTemplate, Map.of(
                "candidateCount", String.valueOf(candidateCount),
                "minAngleKinds", String.valueOf(minAngleKinds),
                "angles", angles(),
                "source", sourceJson
        ));
    }

    public String judge(String sourceJson, String candidatesJson) {
        return render(judgeTemplate, Map.of(
                "angles", angles(),
                "rubric", rubric(),
                "source", sourceJson,
                "candidates", candidatesJson
        ));
    }

    private static String angles() {
        return Arrays.stream(RecommendationAngle.values())
                .map(angle -> "- " + angle.name() + ": " + angle.definition())
                .collect(Collectors.joining("\n"));
    }

    private static String rubric() {
        return "[hard gate: 하나라도 false면 탈락]\n" + checks(true) + "\n[soft: 순위용]\n" + checks(false);
    }

    private static String checks(boolean hard) {
        return Arrays.stream(RecommendationCheck.values())
                .filter(check -> check.hard() == hard)
                .map(check -> "- " + check.name() + ": " + check.question())
                .collect(Collectors.joining("\n"));
    }

    static String render(String template, Map<String, String> values) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            if (value == null) {
                throw new IllegalStateException("프롬프트 템플릿의 값이 없습니다: " + matcher.group(1));
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String read(String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("프롬프트 템플릿을 읽지 못했습니다: " + path, exception);
        }
    }
}
