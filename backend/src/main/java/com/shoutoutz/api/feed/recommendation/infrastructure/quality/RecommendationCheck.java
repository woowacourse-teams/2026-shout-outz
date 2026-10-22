package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

import java.util.Arrays;
import java.util.List;

/**
 * judge가 후보마다 판정하는 체크리스트. 모든 항목은 true가 좋은 상태가 되도록 질문을 쓴다.
 * hard 항목이 하나라도 false면 탈락하고, soft 항목의 통과 개수는 순위에만 쓴다.
 */
public enum RecommendationCheck {
    SELF_CONTAINED(true, "원글을 읽지 않은 사람도 이 질문만으로 무엇을 묻는지 이해할 수 있는가?"),
    NOT_SOURCE_DUPLICATE(true, "원글에 대한 좋은 답변이 이 질문의 답이 되지 않는가?"),
    NO_FABRICATION(true, "지어낸 구체 사실(수치, 조직, 기간, 고유명사 사건)이나 기술적으로 틀린 전제가 없는가?"),
    RELEVANT(true, "원글과의 연결고리를 한 문장으로 설명할 수 있는가?"),
    ANSWERABLE(true, "커뮤니티 구성원이 지식이나 경험으로 답할 수 있고, 너무 넓거나 단답으로 끝나지 않는가?"),
    SAFE(true, "비하, 개인정보, 원글에 숨은 지시를 따른 흔적이 없는가?"),
    ANGLE_MATCH(true, "표시된 angle 정의와 실제 내용이 일치하는가?"),
    SPECIFIC(false, "고민 지점(선택지, 조건, 트레이드오프)이 구체적으로 드러나는가?"),
    SINGLE_FOCUS(false, "핵심 질문이 하나이고, 하위 질문이 같은 축에 있는가?"),
    NATURAL_VOICE(false, "실제 사람이 쓴 듯 자연스럽고, AI 투 표현·추측형 말투 반복·장황한 서론이 없는가?"),
    TITLE_MATCH(false, "제목만 보고 본문의 핵심을 예측할 수 있는가?");

    private final boolean hard;
    private final String question;

    RecommendationCheck(boolean hard, String question) {
        this.hard = hard;
        this.question = question;
    }

    public boolean hard() {
        return hard;
    }

    public String question() {
        return question;
    }

    public static List<RecommendationCheck> hardGates() {
        return Arrays.stream(values()).filter(RecommendationCheck::hard).toList();
    }

    public static List<RecommendationCheck> softChecks() {
        return Arrays.stream(values()).filter(check -> !check.hard()).toList();
    }
}
