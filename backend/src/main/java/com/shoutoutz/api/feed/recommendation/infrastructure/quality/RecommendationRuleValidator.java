package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

import com.shoutoutz.api.feed.recommendation.infrastructure.QuestionRecommendationQualityProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * LLM 평가 전에 길이, 원글 지칭, 금지 토큰, 한국어 비율, 원글 제목 유사도를 결정적으로 검사한다.
 */
public class RecommendationRuleValidator {

    private static final List<String> SOURCE_REFERENCES = List.of(
            "위 질문", "위의 질문", "위 글", "원글", "앞서 말", "작성자님", "질문자님");
    private static final List<String> AI_SELF_MENTIONS = List.of(
            "AI로서", "언어 모델로서", "AI 어시스턴트", "추천 질문");
    private static final Pattern URL = Pattern.compile("https?://|www\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^\\s{0,3}#{1,6}\\s", Pattern.MULTILINE);
    private static final Pattern FENCED_CODE = Pattern.compile("```.*?```", Pattern.DOTALL);
    private static final Pattern INLINE_CODE = Pattern.compile("`[^`]*`");

    private final QuestionRecommendationQualityProperties.Rules rules;

    public RecommendationRuleValidator(QuestionRecommendationQualityProperties.Rules rules) {
        this.rules = rules;
    }

    public RuleValidationResult validate(String sourceTitle, List<RecommendationCandidate> candidates) {
        List<RecommendationCandidate> passed = new ArrayList<>();
        Map<String, RuleViolation> rejected = new LinkedHashMap<>();
        for (RecommendationCandidate candidate : candidates) {
            findViolation(sourceTitle, candidate.title().strip(), candidate.content().strip())
                    .ifPresentOrElse(violation -> rejected.put(candidate.id(), violation),
                            () -> passed.add(candidate));
        }
        return new RuleValidationResult(passed, rejected);
    }

    private Optional<RuleViolation> findViolation(String sourceTitle, String title, String content) {
        if (!hasValidLength(title, content)) {
            return Optional.of(RuleViolation.LENGTH);
        }
        if (containsAny(title, content, SOURCE_REFERENCES)) {
            return Optional.of(RuleViolation.SOURCE_REFERENCE);
        }
        if (hasForbiddenToken(title, content)) {
            return Optional.of(RuleViolation.FORBIDDEN_TOKEN);
        }
        if (!isKorean(title + "\n" + content)) {
            return Optional.of(RuleViolation.NOT_KOREAN);
        }
        if (TextSimilarity.bigramJaccard(sourceTitle, title) > rules.maxSourceTitleSimilarity()) {
            return Optional.of(RuleViolation.SOURCE_SIMILAR);
        }
        return Optional.empty();
    }

    private boolean hasValidLength(String title, String content) {
        int titleLength = title.codePointCount(0, title.length());
        int contentLength = content.codePointCount(0, content.length());
        return titleLength >= rules.minTitleLength() && titleLength <= rules.maxTitleLength()
                && contentLength >= rules.minContentLength() && contentLength <= rules.maxContentLength();
    }

    private boolean hasForbiddenToken(String title, String content) {
        String text = title + "\n" + content;
        return URL.matcher(text).find()
                || EMAIL.matcher(text).find()
                || MARKDOWN_HEADING.matcher(content).find()
                || containsAny(title, content, AI_SELF_MENTIONS);
    }

    private static boolean containsAny(String title, String content, List<String> phrases) {
        return phrases.stream().anyMatch(phrase -> title.contains(phrase) || content.contains(phrase));
    }

    private boolean isKorean(String text) {
        String withoutCode = INLINE_CODE.matcher(FENCED_CODE.matcher(text).replaceAll(" ")).replaceAll(" ");
        long letters = withoutCode.codePoints().filter(Character::isLetter).count();
        if (letters == 0) {
            return false;
        }
        long korean = withoutCode.codePoints()
                .filter(Character::isLetter)
                .filter(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HANGUL)
                .count();
        return (double) korean / letters >= rules.minKoreanRatio();
    }
}
