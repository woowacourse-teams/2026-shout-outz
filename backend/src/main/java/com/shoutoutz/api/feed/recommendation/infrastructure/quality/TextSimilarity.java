package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 형태소 분석 없이 한국어 문장의 표면 유사도를 비교하기 위한 문자 bigram Jaccard 유사도.
 */
public final class TextSimilarity {

    private TextSimilarity() {
    }

    public static double bigramJaccard(String left, String right) {
        Set<String> leftBigrams = bigrams(left);
        Set<String> rightBigrams = bigrams(right);
        if (leftBigrams.isEmpty() && rightBigrams.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(leftBigrams);
        intersection.retainAll(rightBigrams);
        Set<String> union = new HashSet<>(leftBigrams);
        union.addAll(rightBigrams);
        return (double) intersection.size() / union.size();
    }

    private static Set<String> bigrams(String text) {
        if (text == null) {
            return Set.of();
        }
        int[] codePoints = text.toLowerCase(Locale.ROOT).codePoints()
                .filter(codePoint -> Character.isLetterOrDigit(codePoint))
                .toArray();
        Set<String> result = new HashSet<>();
        for (int index = 0; index + 1 < codePoints.length; index++) {
            result.add(new String(codePoints, index, 2));
        }
        return result;
    }
}
